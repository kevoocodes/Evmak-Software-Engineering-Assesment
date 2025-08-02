-- Car Parking Management System Database Schema
-- Optimized for high performance and concurrent operations

-- Users table for authentication and user management
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('ADMIN', 'USER', 'PARKING_ATTENDANT') NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB;

-- Vehicles registered by users
CREATE TABLE vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    make VARCHAR(50),
    model VARCHAR(50),
    color VARCHAR(30),
    vehicle_type ENUM('CAR', 'MOTORCYCLE', 'TRUCK', 'VAN') NOT NULL DEFAULT 'CAR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_vehicles_user_id (user_id),
    INDEX idx_vehicles_license_plate (license_plate),
    INDEX idx_vehicles_active (is_active)
) ENGINE=InnoDB;

-- Parking facilities (garages and street zones)
CREATE TABLE parking_facilities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    facility_type ENUM('GARAGE', 'STREET_ZONE') NOT NULL,
    address VARCHAR(255) NOT NULL,
    location_lat DECIMAL(10, 8) NOT NULL,
    location_lng DECIMAL(11, 8) NOT NULL,
    total_spots INT NOT NULL DEFAULT 0,
    available_spots INT NOT NULL DEFAULT 0,
    base_hourly_rate DECIMAL(8, 2) NOT NULL,
    max_hours INT DEFAULT 24,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    operating_hours_start TIME DEFAULT '00:00:00',
    operating_hours_end TIME DEFAULT '23:59:59',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_facilities_type (facility_type),
    INDEX idx_facilities_location (location_lat, location_lng),
    INDEX idx_facilities_active (is_active),
    INDEX idx_facilities_available_spots (available_spots),
    SPATIAL INDEX idx_facilities_spatial_location (POINT(location_lng, location_lat))
) ENGINE=InnoDB;

-- Individual parking spots within facilities
CREATE TABLE parking_spots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    facility_id BIGINT NOT NULL,
    spot_number VARCHAR(20) NOT NULL,
    spot_type ENUM('REGULAR', 'DISABLED', 'ELECTRIC', 'COMPACT') NOT NULL DEFAULT 'REGULAR',
    floor_level INT DEFAULT 0,
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'OUT_OF_ORDER') NOT NULL DEFAULT 'AVAILABLE',
    reserved_by BIGINT NULL,
    reservation_expires_at TIMESTAMP NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (facility_id) REFERENCES parking_facilities(id) ON DELETE CASCADE,
    FOREIGN KEY (reserved_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uk_spot_facility (facility_id, spot_number),
    INDEX idx_spots_facility_id (facility_id),
    INDEX idx_spots_status (status),
    INDEX idx_spots_reserved_by (reserved_by),
    INDEX idx_spots_reservation_expires (reservation_expires_at),
    INDEX idx_spots_facility_status (facility_id, status),
    INDEX idx_spots_last_updated (last_updated)
) ENGINE=InnoDB;

-- Dynamic pricing rules based on demand, time, and events
CREATE TABLE pricing_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    facility_id BIGINT NULL, -- NULL means applies to all facilities
    rule_name VARCHAR(100) NOT NULL,
    rule_type ENUM('TIME_BASED', 'DEMAND_BASED', 'EVENT_BASED', 'WEATHER_BASED') NOT NULL,
    priority INT NOT NULL DEFAULT 0, -- Higher priority rules override lower ones
    start_time TIME,
    end_time TIME,
    days_of_week SET('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'),
    demand_threshold_percentage DECIMAL(5, 2), -- e.g., 80.00 for 80% occupancy
    multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.00, -- Price multiplier
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (facility_id) REFERENCES parking_facilities(id) ON DELETE CASCADE,
    INDEX idx_pricing_facility_id (facility_id),
    INDEX idx_pricing_type (rule_type),
    INDEX idx_pricing_active (is_active),
    INDEX idx_pricing_priority (priority DESC)
) ENGINE=InnoDB;

-- Parking sessions (when a user parks their vehicle)
CREATE TABLE parking_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    session_reference VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    planned_duration_hours INT,
    actual_duration_minutes INT,
    hourly_rate DECIMAL(8, 2) NOT NULL,
    total_amount DECIMAL(10, 2),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (spot_id) REFERENCES parking_spots(id) ON DELETE CASCADE,
    INDEX idx_sessions_user_id (user_id),
    INDEX idx_sessions_vehicle_id (vehicle_id),
    INDEX idx_sessions_spot_id (spot_id),
    INDEX idx_sessions_status (status),
    INDEX idx_sessions_reference (session_reference),
    INDEX idx_sessions_started_at (started_at),
    INDEX idx_sessions_ended_at (ended_at),
    INDEX idx_sessions_active_spot (spot_id, status),
    INDEX idx_sessions_date_range (started_at, ended_at)
) ENGINE=InnoDB;

-- Payment transactions
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    payment_reference VARCHAR(100) NOT NULL UNIQUE,
    external_payment_id VARCHAR(255), -- Payment provider transaction ID
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    payment_method ENUM('CARD', 'MOBILE_MONEY', 'CASH') NOT NULL,
    payment_provider VARCHAR(50), -- e.g., 'X-PAYMENT-PROVIDER', 'VISA', 'VodaCom'
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    payment_data JSON, -- Store provider-specific payment details
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    FOREIGN KEY (session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE,
    INDEX idx_payments_session_id (session_id),
    INDEX idx_payments_reference (payment_reference),
    INDEX idx_payments_external_id (external_payment_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_method (payment_method),
    INDEX idx_payments_created_at (created_at)
) ENGINE=InnoDB;

-- Parking violations and fines
CREATE TABLE violations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NULL, -- NULL if violation not related to a session
    spot_id BIGINT NOT NULL,
    vehicle_license_plate VARCHAR(20) NOT NULL,
    violation_type ENUM('OVERSTAY', 'NO_PAYMENT', 'UNAUTHORIZED_PARKING', 'DISABLED_SPOT_MISUSE') NOT NULL,
    fine_amount DECIMAL(8, 2) NOT NULL,
    status ENUM('PENDING', 'PAID', 'DISPUTED', 'WAIVED') NOT NULL DEFAULT 'PENDING',
    description TEXT,
    reported_by BIGINT, -- User ID of reporting officer
    evidence_url VARCHAR(500), -- Photo or document URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (session_id) REFERENCES parking_sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (spot_id) REFERENCES parking_spots(id) ON DELETE CASCADE,
    FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_violations_session_id (session_id),
    INDEX idx_violations_spot_id (spot_id),
    INDEX idx_violations_license_plate (vehicle_license_plate),
    INDEX idx_violations_type (violation_type),
    INDEX idx_violations_status (status),
    INDEX idx_violations_created_at (created_at)
) ENGINE=InnoDB;

-- Reservation system for future parking
CREATE TABLE reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    spot_id BIGINT NULL, -- NULL for any available spot in facility
    reservation_reference VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('ACTIVE', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    reserved_from TIMESTAMP NOT NULL,
    reserved_until TIMESTAMP NOT NULL,
    hourly_rate DECIMAL(8, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    expires_at TIMESTAMP NOT NULL, -- When reservation automatically expires
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (facility_id) REFERENCES parking_facilities(id) ON DELETE CASCADE,
    FOREIGN KEY (spot_id) REFERENCES parking_spots(id) ON DELETE SET NULL,
    INDEX idx_reservations_user_id (user_id),
    INDEX idx_reservations_vehicle_id (vehicle_id),
    INDEX idx_reservations_facility_id (facility_id),
    INDEX idx_reservations_spot_id (spot_id),
    INDEX idx_reservations_reference (reservation_reference),
    INDEX idx_reservations_status (status),
    INDEX idx_reservations_time_range (reserved_from, reserved_until),
    INDEX idx_reservations_expires_at (expires_at)
) ENGINE=InnoDB;

-- System audit log for tracking all critical operations
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL, -- e.g., 'parking_session', 'payment'
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- e.g., 'CREATE', 'UPDATE', 'DELETE'
    user_id BIGINT,
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB;

-- Performance optimization: Create a materialized view-like table for availability
CREATE TABLE facility_availability_cache (
    facility_id BIGINT PRIMARY KEY,
    total_spots INT NOT NULL,
    available_spots INT NOT NULL,
    occupied_spots INT NOT NULL,
    reserved_spots INT NOT NULL,
    out_of_order_spots INT NOT NULL,
    occupancy_percentage DECIMAL(5, 2) NOT NULL,
    current_hourly_rate DECIMAL(8, 2) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (facility_id) REFERENCES parking_facilities(id) ON DELETE CASCADE,
    INDEX idx_availability_occupancy (occupancy_percentage),
    INDEX idx_availability_available (available_spots),
    INDEX idx_availability_last_updated (last_updated)
) ENGINE=InnoDB;