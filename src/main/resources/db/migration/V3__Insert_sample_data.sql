-- Sample data for Car Parking Management System
-- This creates realistic test data for development and testing

-- Insert sample users
INSERT INTO users (username, email, password_hash, phone_number, first_name, last_name, role, is_active) VALUES
('admin', 'admin@parking.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '+255712345001', 'Admin', 'User', 'ADMIN', true),
('john_doe', 'john.doe@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '+255712345002', 'John', 'Doe', 'USER', true),
('jane_smith', 'jane.smith@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '+255712345003', 'Jane', 'Smith', 'USER', true),
('attendant1', 'attendant@parking.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '+255712345004', 'Park', 'Attendant', 'PARKING_ATTENDANT', true),
('mike_wilson', 'mike.wilson@email.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '+255712345005', 'Mike', 'Wilson', 'USER', true);

-- Insert sample vehicles
INSERT INTO vehicles (user_id, license_plate, make, model, color, vehicle_type, is_active) VALUES
(2, 'T123ABC', 'Toyota', 'Corolla', 'Blue', 'CAR', true),
(2, 'T456DEF', 'Honda', 'Civic', 'Red', 'CAR', true),
(3, 'T789GHI', 'Nissan', 'Altima', 'White', 'CAR', true),
(3, 'T321JKL', 'Ford', 'Explorer', 'Black', 'VAN', true),
(5, 'T654MNO', 'BMW', 'X5', 'Silver', 'CAR', true),
(5, 'T987PQR', 'Mercedes', 'C-Class', 'Gray', 'CAR', true);

-- Insert parking facilities (15 garages + some street zones)
INSERT INTO parking_facilities (name, facility_type, address, location_lat, location_lng, total_spots, available_spots, base_hourly_rate, max_hours, is_active) VALUES
-- Downtown Garages
('City Center Garage', 'GARAGE', '123 Main Street, Dar es Salaam', -6.7924, 39.2083, 200, 150, 2000.00, 24, true),
('Business District Parking', 'GARAGE', '456 Commerce Ave, Dar es Salaam', -6.7945, 39.2105, 180, 120, 2500.00, 12, true),
('Shopping Mall Garage', 'GARAGE', '789 Shopping Blvd, Dar es Salaam', -6.7935, 39.2095, 300, 250, 1500.00, 24, true),
('Hotel Plaza Parking', 'GARAGE', '321 Hotel Street, Dar es Salaam', -6.7955, 39.2115, 150, 100, 3000.00, 24, true),
('Government Complex', 'GARAGE', '654 Government Road, Dar es Salaam', -6.7965, 39.2125, 120, 80, 1800.00, 10, true),

-- Airport and Transport Hubs
('Airport Terminal Garage', 'GARAGE', 'Julius Nyerere Airport, Dar es Salaam', -6.8781, 39.2026, 500, 400, 5000.00, 72, true),
('Central Bus Station', 'GARAGE', 'Ubungo Bus Terminal, Dar es Salaam', -6.7774, 39.2395, 250, 200, 1000.00, 24, true),
('Ferry Terminal Parking', 'GARAGE', 'Kivukoni Ferry Terminal, Dar es Salaam', -6.8235, 39.2675, 100, 75, 2200.00, 12, true),

-- Hospital and University Areas
('Muhimbili Hospital Garage', 'GARAGE', 'Muhimbili University, Dar es Salaam', -6.7874, 39.2559, 180, 140, 1200.00, 24, true),
('University of Dar es Salaam', 'GARAGE', 'University Road, Dar es Salaam', -6.7756, 39.2086, 300, 280, 800.00, 24, true),

-- Residential and Mixed Use
('Masaki Residential Complex', 'GARAGE', 'Masaki Peninsula, Dar es Salaam', -6.7574, 39.2723, 150, 120, 1500.00, 24, true),
('Mikocheni Plaza', 'GARAGE', 'Mikocheni Area, Dar es Salaam', -6.7574, 39.2189, 120, 100, 1800.00, 24, true),
('Sinza Community Center', 'GARAGE', 'Sinza Area, Dar es Salaam', -6.7324, 39.2489, 80, 70, 1000.00, 24, true),
('Tegeta Beach Resort', 'GARAGE', 'Tegeta Beach, Dar es Salaam', -6.6574, 39.1189, 100, 90, 2500.00, 24, true),
('Mlimani City Mall', 'GARAGE', 'Sam Nujoma Road, Dar es Salaam', -6.7724, 39.2389, 400, 350, 1800.00, 24, true),

-- Street Zones
('CBD Street Zone A', 'STREET_ZONE', 'Samora Avenue, Dar es Salaam', -6.8162, 39.2844, 50, 35, 1500.00, 4, true),
('CBD Street Zone B', 'STREET_ZONE', 'Kivukoni Front, Dar es Salaam', -6.8199, 39.2882, 40, 25, 1800.00, 4, true),
('Oyster Bay Street Zone', 'STREET_ZONE', 'Haile Selassie Road, Dar es Salaam', -6.7674, 39.2789, 30, 20, 2000.00, 3, true),
('Msimbazi Street Zone', 'STREET_ZONE', 'Msimbazi Street, Dar es Salaam', -6.8074, 39.2689, 35, 30, 1200.00, 6, true),
('Posta Street Zone', 'STREET_ZONE', 'Posta Street, Dar es Salaam', -6.8124, 39.2789, 25, 20, 1600.00, 4, true);

-- Create parking spots for the first few garages (sample)
-- City Center Garage (200 spots)
INSERT INTO parking_spots (facility_id, spot_number, spot_type, floor_level, status) VALUES
-- Ground Floor (50 spots)
(1, 'G001', 'DISABLED', 0, 'AVAILABLE'), (1, 'G002', 'DISABLED', 0, 'AVAILABLE'), (1, 'G003', 'REGULAR', 0, 'AVAILABLE'),
(1, 'G004', 'REGULAR', 0, 'AVAILABLE'), (1, 'G005', 'REGULAR', 0, 'AVAILABLE'), (1, 'G006', 'ELECTRIC', 0, 'AVAILABLE'),
(1, 'G007', 'ELECTRIC', 0, 'AVAILABLE'), (1, 'G008', 'REGULAR', 0, 'AVAILABLE'), (1, 'G009', 'REGULAR', 0, 'AVAILABLE'),
(1, 'G010', 'COMPACT', 0, 'AVAILABLE');

-- Continue with more spots for testing (abbreviated for readability)
-- First Floor (50 spots)
INSERT INTO parking_spots (facility_id, spot_number, spot_type, floor_level, status)
SELECT 1, CONCAT('F1', LPAD(seq, 3, '0')), 
       CASE 
           WHEN seq <= 5 THEN 'ELECTRIC'
           WHEN seq <= 15 THEN 'COMPACT'
           ELSE 'REGULAR'
       END,
       1, 'AVAILABLE'
FROM (
    SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
    SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION
    SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION
    SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION
    SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION
    SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION
    SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION
    SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION
    SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION
    SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) AS seq_table;

-- Business District Parking (180 spots)
INSERT INTO parking_spots (facility_id, spot_number, spot_type, floor_level, status)
SELECT 2, CONCAT('BD', LPAD(seq, 3, '0')), 
       CASE 
           WHEN seq <= 10 THEN 'DISABLED'
           WHEN seq <= 25 THEN 'ELECTRIC'
           WHEN seq <= 60 THEN 'COMPACT'
           ELSE 'REGULAR'
       END,
       FLOOR((seq - 1) / 60), 'AVAILABLE'
FROM (
    SELECT @row_number := @row_number + 1 AS seq
    FROM information_schema.tables t1, information_schema.tables t2, (SELECT @row_number := 0) r
    LIMIT 180
) AS seq_table;

-- Shopping Mall Garage (300 spots)
INSERT INTO parking_spots (facility_id, spot_number, spot_type, floor_level, status)
SELECT 3, CONCAT('SM', LPAD(seq, 3, '0')), 
       CASE 
           WHEN seq <= 15 THEN 'DISABLED'
           WHEN seq <= 40 THEN 'ELECTRIC'
           WHEN seq <= 100 THEN 'COMPACT'
           ELSE 'REGULAR'
       END,
       FLOOR((seq - 1) / 75), 'AVAILABLE'
FROM (
    SELECT @row_number2 := @row_number2 + 1 AS seq
    FROM information_schema.tables t1, information_schema.tables t2, (SELECT @row_number2 := 0) r
    LIMIT 300
) AS seq_table;

-- Insert some pricing rules
INSERT INTO pricing_rules (facility_id, rule_name, rule_type, priority, start_time, end_time, days_of_week, multiplier, is_active) VALUES
-- Peak hour pricing
(NULL, 'Morning Rush Hour', 'TIME_BASED', 100, '07:00:00', '09:00:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', 1.50, true),
(NULL, 'Evening Rush Hour', 'TIME_BASED', 100, '17:00:00', '19:00:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', 1.50, true),
(NULL, 'Weekend Premium', 'TIME_BASED', 80, '10:00:00', '22:00:00', 'SATURDAY,SUNDAY', 1.25, true),

-- Demand-based pricing
(NULL, 'High Demand Surge', 'DEMAND_BASED', 200, NULL, NULL, NULL, 2.00, true),
(NULL, 'Medium Demand', 'DEMAND_BASED', 150, NULL, NULL, NULL, 1.30, true),

-- Facility-specific rules
(1, 'City Center Premium', 'TIME_BASED', 90, '08:00:00', '18:00:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', 1.75, true),
(6, 'Airport Long Stay Discount', 'TIME_BASED', 50, '00:00:00', '23:59:59', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 0.80, true);

-- Set demand thresholds for demand-based rules
UPDATE pricing_rules SET demand_threshold_percentage = 85.00 WHERE rule_name = 'High Demand Surge';
UPDATE pricing_rules SET demand_threshold_percentage = 70.00 WHERE rule_name = 'Medium Demand';

-- Initialize facility availability cache
INSERT INTO facility_availability_cache (facility_id, total_spots, available_spots, occupied_spots, reserved_spots, out_of_order_spots, occupancy_percentage, current_hourly_rate)
SELECT 
    pf.id,
    pf.total_spots,
    pf.available_spots,
    pf.total_spots - pf.available_spots,
    0,
    0,
    CASE WHEN pf.total_spots > 0 THEN ((pf.total_spots - pf.available_spots) * 100.0 / pf.total_spots) ELSE 0 END,
    pf.base_hourly_rate
FROM parking_facilities pf;

-- Create some sample reservations and sessions (for testing)
INSERT INTO reservations (user_id, vehicle_id, facility_id, spot_id, reservation_reference, status, reserved_from, reserved_until, hourly_rate, total_amount, expires_at) VALUES
(2, 1, 1, 1, 'RES-001', 'ACTIVE', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR), 2000.00, 4000.00, DATE_ADD(NOW(), INTERVAL 15 MINUTE)),
(3, 3, 2, 181, 'RES-002', 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), 2500.00, 7500.00, DATE_ADD(NOW(), INTERVAL 30 MINUTE));

-- Create some active parking sessions
INSERT INTO parking_sessions (user_id, vehicle_id, spot_id, session_reference, status, planned_duration_hours, hourly_rate, started_at) VALUES
(2, 1, 11, 'PARK-001', 'ACTIVE', 2, 2000.00, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(3, 3, 182, 'PARK-002', 'ACTIVE', 4, 2500.00, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 5, 481, 'PARK-003', 'ACTIVE', 1, 1500.00, DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- Update spot status for active sessions
UPDATE parking_spots SET status = 'OCCUPIED' WHERE id IN (11, 182, 481);

-- Create some completed sessions with payments
INSERT INTO parking_sessions (user_id, vehicle_id, spot_id, session_reference, status, planned_duration_hours, actual_duration_minutes, hourly_rate, total_amount, started_at, ended_at) VALUES
(2, 2, 12, 'PARK-004', 'COMPLETED', 2, 135, 2000.00, 6000.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(3, 4, 183, 'PARK-005', 'COMPLETED', 1, 75, 2500.00, 2500.00, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- Create payments for completed sessions
INSERT INTO payments (session_id, payment_reference, amount, currency, payment_method, payment_provider, status, completed_at) VALUES
(4, 'PAY-001', 6000.00, 'TZS', 'CARD', 'X-PAYMENT-PROVIDER', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(5, 'PAY-002', 2500.00, 'TZS', 'MOBILE_MONEY', 'VodaCom', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- Create some violations
INSERT INTO violations (session_id, spot_id, vehicle_license_plate, violation_type, fine_amount, status, description, reported_by) VALUES
(NULL, 50, 'T999XYZ', 'NO_PAYMENT', 10000.00, 'PENDING', 'Vehicle parked without payment for 2 hours', 4),
(NULL, 1, 'T888ABC', 'DISABLED_SPOT_MISUSE', 25000.00, 'PENDING', 'Regular vehicle using disabled parking spot', 4);

-- Update available spots count based on occupied spots
UPDATE parking_facilities pf 
SET available_spots = pf.total_spots - (
    SELECT COUNT(*) 
    FROM parking_spots ps 
    WHERE ps.facility_id = pf.id 
    AND ps.status IN ('OCCUPIED', 'RESERVED')
);