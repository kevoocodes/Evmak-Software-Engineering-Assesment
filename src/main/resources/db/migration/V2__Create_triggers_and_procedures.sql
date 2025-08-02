-- Triggers and Stored Procedures for maintaining data consistency and performance

DELIMITER $$

-- Trigger to update facility availability when spot status changes
CREATE TRIGGER update_facility_availability_after_spot_update
    AFTER UPDATE ON parking_spots
    FOR EACH ROW
BEGIN
    -- Only update if status actually changed
    IF OLD.status != NEW.status THEN
        CALL update_facility_availability_cache(NEW.facility_id);
    END IF;
END$$

-- Trigger to update facility availability when new spot is added
CREATE TRIGGER update_facility_availability_after_spot_insert
    AFTER INSERT ON parking_spots
    FOR EACH ROW
BEGIN
    CALL update_facility_availability_cache(NEW.facility_id);
END$$

-- Trigger to update facility availability when spot is deleted
CREATE TRIGGER update_facility_availability_after_spot_delete
    AFTER DELETE ON parking_spots
    FOR EACH ROW
BEGIN
    CALL update_facility_availability_cache(OLD.facility_id);
END$$

-- Trigger to automatically expire reservations
CREATE TRIGGER check_reservation_expiry_before_spot_update
    BEFORE UPDATE ON parking_spots
    FOR EACH ROW
BEGIN
    -- If spot is reserved but reservation has expired, set to AVAILABLE
    IF NEW.status = 'RESERVED' 
       AND NEW.reservation_expires_at IS NOT NULL 
       AND NEW.reservation_expires_at < NOW() THEN
        SET NEW.status = 'AVAILABLE';
        SET NEW.reserved_by = NULL;
        SET NEW.reservation_expires_at = NULL;
    END IF;
END$$

-- Trigger to calculate parking session duration and amount
CREATE TRIGGER calculate_session_totals_before_update
    BEFORE UPDATE ON parking_sessions
    FOR EACH ROW
BEGIN
    -- Calculate duration when session ends
    IF OLD.status = 'ACTIVE' AND NEW.status IN ('COMPLETED', 'CANCELLED') AND NEW.ended_at IS NOT NULL THEN
        SET NEW.actual_duration_minutes = TIMESTAMPDIFF(MINUTE, NEW.started_at, NEW.ended_at);
        
        -- Calculate total amount based on actual duration
        IF NEW.status = 'COMPLETED' THEN
            SET NEW.total_amount = CEIL(NEW.actual_duration_minutes / 60.0) * NEW.hourly_rate;
        END IF;
    END IF;
END$$

-- Trigger to update spot status when session starts/ends
CREATE TRIGGER update_spot_status_after_session_change
    AFTER UPDATE ON parking_sessions
    FOR EACH ROW
BEGIN
    -- When session becomes active, mark spot as occupied
    IF OLD.status != 'ACTIVE' AND NEW.status = 'ACTIVE' THEN
        UPDATE parking_spots 
        SET status = 'OCCUPIED', 
            reserved_by = NULL, 
            reservation_expires_at = NULL 
        WHERE id = NEW.spot_id;
    END IF;
    
    -- When session ends, mark spot as available
    IF OLD.status = 'ACTIVE' AND NEW.status IN ('COMPLETED', 'CANCELLED', 'EXPIRED') THEN
        UPDATE parking_spots 
        SET status = 'AVAILABLE' 
        WHERE id = NEW.spot_id;
    END IF;
END$$

-- Stored procedure to update facility availability cache
CREATE PROCEDURE update_facility_availability_cache(IN facility_id_param BIGINT)
BEGIN
    DECLARE total_count INT DEFAULT 0;
    DECLARE available_count INT DEFAULT 0;
    DECLARE occupied_count INT DEFAULT 0;
    DECLARE reserved_count INT DEFAULT 0;
    DECLARE out_of_order_count INT DEFAULT 0;
    DECLARE occupancy_pct DECIMAL(5,2) DEFAULT 0.00;
    DECLARE current_rate DECIMAL(8,2) DEFAULT 0.00;
    
    -- Get spot counts
    SELECT 
        COUNT(*),
        SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END),
        SUM(CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN status = 'OUT_OF_ORDER' THEN 1 ELSE 0 END)
    INTO total_count, available_count, occupied_count, reserved_count, out_of_order_count
    FROM parking_spots
    WHERE facility_id = facility_id_param;
    
    -- Calculate occupancy percentage
    IF total_count > 0 THEN
        SET occupancy_pct = ((occupied_count + reserved_count) / total_count) * 100.0;
    END IF;
    
    -- Get current rate (simplified - would include pricing rules logic)
    SELECT base_hourly_rate INTO current_rate
    FROM parking_facilities
    WHERE id = facility_id_param;
    
    -- Update cache
    INSERT INTO facility_availability_cache (
        facility_id, total_spots, available_spots, occupied_spots, 
        reserved_spots, out_of_order_spots, occupancy_percentage, current_hourly_rate
    ) VALUES (
        facility_id_param, total_count, available_count, occupied_count,
        reserved_count, out_of_order_count, occupancy_pct, current_rate
    ) ON DUPLICATE KEY UPDATE
        total_spots = total_count,
        available_spots = available_count,
        occupied_spots = occupied_count,
        reserved_spots = reserved_count,
        out_of_order_spots = out_of_order_count,
        occupancy_percentage = occupancy_pct,
        current_hourly_rate = current_rate;
    
    -- Update the main facility table
    UPDATE parking_facilities 
    SET available_spots = available_count, total_spots = total_count
    WHERE id = facility_id_param;
END$$

-- Stored procedure for high-performance spot search
CREATE PROCEDURE find_available_spots_nearby(
    IN search_lat DECIMAL(10,8),
    IN search_lng DECIMAL(11,8),
    IN radius_meters INT,
    IN spot_type_filter VARCHAR(20),
    IN max_results INT
)
BEGIN
    SELECT 
        pf.id as facility_id,
        pf.name as facility_name,
        pf.facility_type,
        pf.address,
        pf.location_lat,
        pf.location_lng,
        fac.available_spots,
        fac.current_hourly_rate,
        ST_Distance_Sphere(
            POINT(pf.location_lng, pf.location_lat),
            POINT(search_lng, search_lat)
        ) as distance_meters
    FROM parking_facilities pf
    JOIN facility_availability_cache fac ON pf.id = fac.facility_id
    WHERE pf.is_active = TRUE
      AND fac.available_spots > 0
      AND ST_Distance_Sphere(
          POINT(pf.location_lng, pf.location_lat),
          POINT(search_lng, search_lat)
      ) <= radius_meters
      AND (spot_type_filter IS NULL OR EXISTS (
          SELECT 1 FROM parking_spots ps 
          WHERE ps.facility_id = pf.id 
            AND ps.spot_type = spot_type_filter 
            AND ps.status = 'AVAILABLE'
      ))
    ORDER BY distance_meters ASC, fac.available_spots DESC
    LIMIT max_results;
END$$

-- Stored procedure for atomic spot reservation
CREATE PROCEDURE reserve_parking_spot(
    IN user_id_param BIGINT,
    IN spot_id_param BIGINT,
    IN reservation_minutes INT,
    OUT success BOOLEAN,
    OUT message VARCHAR(255),
    OUT reservation_id BIGINT
)
BEGIN
    DECLARE spot_status VARCHAR(20);
    DECLARE spot_reserved_by BIGINT;
    DECLARE spot_reservation_expires TIMESTAMP;
    DECLARE facility_id_val BIGINT;
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET success = FALSE;
        SET message = 'Database error occurred during reservation';
        SET reservation_id = NULL;
    END;
    
    START TRANSACTION;
    
    -- Lock the spot for update
    SELECT status, reserved_by, reservation_expires_at, facility_id
    INTO spot_status, spot_reserved_by, spot_reservation_expires, facility_id_val
    FROM parking_spots
    WHERE id = spot_id_param
    FOR UPDATE;
    
    -- Check if spot is available
    IF spot_status != 'AVAILABLE' THEN
        SET success = FALSE;
        SET message = CONCAT('Spot is not available. Current status: ', spot_status);
        SET reservation_id = NULL;
        ROLLBACK;
    ELSE
        -- Reserve the spot
        UPDATE parking_spots
        SET status = 'RESERVED',
            reserved_by = user_id_param,
            reservation_expires_at = DATE_ADD(NOW(), INTERVAL reservation_minutes MINUTE)
        WHERE id = spot_id_param;
        
        -- Create reservation record
        INSERT INTO reservations (
            user_id, facility_id, spot_id, reservation_reference,
            reserved_from, reserved_until, expires_at,
            hourly_rate, total_amount
        ) VALUES (
            user_id_param, facility_id_val, spot_id_param, 
            CONCAT('RES-', UNIX_TIMESTAMP(), '-', spot_id_param),
            NOW(), DATE_ADD(NOW(), INTERVAL reservation_minutes MINUTE),
            DATE_ADD(NOW(), INTERVAL 15 MINUTE), -- 15 minute hold
            (SELECT base_hourly_rate FROM parking_facilities WHERE id = facility_id_val),
            0.00 -- Will be calculated when session starts
        );
        
        SET reservation_id = LAST_INSERT_ID();
        SET success = TRUE;
        SET message = 'Spot reserved successfully';
        
        COMMIT;
    END IF;
END$$

-- Stored procedure to clean up expired reservations
CREATE PROCEDURE cleanup_expired_reservations()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE spot_id_val BIGINT;
    DECLARE cur CURSOR FOR 
        SELECT DISTINCT spot_id 
        FROM parking_spots 
        WHERE status = 'RESERVED' 
          AND reservation_expires_at < NOW();
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- Update expired reservations to cancelled
    UPDATE reservations 
    SET status = 'EXPIRED' 
    WHERE status = 'ACTIVE' 
      AND expires_at < NOW();
    
    -- Free up spots with expired reservations
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO spot_id_val;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        UPDATE parking_spots
        SET status = 'AVAILABLE',
            reserved_by = NULL,
            reservation_expires_at = NULL
        WHERE id = spot_id_val
          AND status = 'RESERVED'
          AND reservation_expires_at < NOW();
    END LOOP;
    
    CLOSE cur;
END$$

DELIMITER ;

-- Create event to run cleanup every 5 minutes
SET GLOBAL event_scheduler = ON;

CREATE EVENT IF NOT EXISTS cleanup_expired_reservations_event
ON SCHEDULE EVERY 5 MINUTE
DO
    CALL cleanup_expired_reservations();