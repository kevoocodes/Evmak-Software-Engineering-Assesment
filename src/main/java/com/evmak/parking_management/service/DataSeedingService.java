package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.*;
import com.evmak.parking_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataSeedingService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private ParkingFacilityRepository facilityRepository;
    
    @Autowired
    private ParkingSpotRepository spotRepository;
    
    @Autowired
    private ParkingSessionRepository sessionRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    public void seedData() {
        if (userRepository.count() == 0) {
            seedDatabase();
        }
    }

    private void seedDatabase() {
        System.out.println("🌱 Seeding database with sample data...");
        
        // Create Users
        User admin = createUser("admin", "admin@parking.com", "Admin", "User", User.UserRole.ADMIN);
        User john = createUser("john_doe", "john.doe@email.com", "John", "Doe", User.UserRole.USER);
        User jane = createUser("jane_smith", "jane.smith@email.com", "Jane", "Smith", User.UserRole.USER);
        User attendant = createUser("attendant1", "attendant@parking.com", "Park", "Attendant", User.UserRole.PARKING_ATTENDANT);
        User mike = createUser("mike_wilson", "mike.wilson@email.com", "Mike", "Wilson", User.UserRole.USER);
        
        // Create Vehicles
        Vehicle vehicle1 = createVehicle(john, "T123ABC", "Toyota", "Corolla", "Blue", Vehicle.VehicleType.CAR);
        Vehicle vehicle2 = createVehicle(john, "T456DEF", "Honda", "Civic", "Red", Vehicle.VehicleType.CAR);
        Vehicle vehicle3 = createVehicle(jane, "T789GHI", "Nissan", "Altima", "White", Vehicle.VehicleType.CAR);
        Vehicle vehicle4 = createVehicle(jane, "T321JKL", "Ford", "Explorer", "Black", Vehicle.VehicleType.VAN);
        Vehicle vehicle5 = createVehicle(mike, "T654MNO", "BMW", "X5", "Silver", Vehicle.VehicleType.CAR);
        
        // Create Parking Facilities
        ParkingFacility cityCenter = createFacility(
            "City Center Garage", ParkingFacility.FacilityType.GARAGE,
            "123 Main Street, Dar es Salaam", 
            new BigDecimal("-6.7924"), new BigDecimal("39.2083"), 
            new BigDecimal("2000.00"), 200
        );
        
        ParkingFacility businessDistrict = createFacility(
            "Business District Parking", ParkingFacility.FacilityType.GARAGE,
            "456 Commerce Ave, Dar es Salaam", 
            new BigDecimal("-6.7945"), new BigDecimal("39.2105"), 
            new BigDecimal("2500.00"), 180
        );
        
        ParkingFacility shoppingMall = createFacility(
            "Shopping Mall Garage", ParkingFacility.FacilityType.GARAGE,
            "789 Shopping Blvd, Dar es Salaam", 
            new BigDecimal("-6.7935"), new BigDecimal("39.2095"), 
            new BigDecimal("1500.00"), 300
        );
        
        ParkingFacility streetZoneA = createFacility(
            "CBD Street Zone A", ParkingFacility.FacilityType.STREET_ZONE,
            "Samora Avenue, Dar es Salaam", 
            new BigDecimal("-6.8162"), new BigDecimal("39.2844"), 
            new BigDecimal("1500.00"), 50
        );
        
        ParkingFacility airport = createFacility(
            "Airport Terminal Garage", ParkingFacility.FacilityType.GARAGE,
            "Julius Nyerere Airport, Dar es Salaam", 
            new BigDecimal("-6.8781"), new BigDecimal("39.2026"), 
            new BigDecimal("5000.00"), 500
        );
        
        // Create Parking Spots
        createSpotsForFacility(cityCenter, 200);
        createSpotsForFacility(businessDistrict, 180);
        createSpotsForFacility(shoppingMall, 300);
        createSpotsForFacility(streetZoneA, 50);
        createSpotsForFacility(airport, 500);
        
        // Create some active parking sessions
        ParkingSpot spot1 = spotRepository.findAvailableSpotsInFacility(cityCenter.getId()).get(0);
        ParkingSpot spot2 = spotRepository.findAvailableSpotsInFacility(businessDistrict.getId()).get(0);
        
        ParkingSession session1 = createActiveSession(john, vehicle1, spot1, new BigDecimal("2000.00"));
        ParkingSession session2 = createActiveSession(jane, vehicle3, spot2, new BigDecimal("2500.00"));
        
        // Create completed sessions with payments
        ParkingSpot spot3 = spotRepository.findAvailableSpotsInFacility(shoppingMall.getId()).get(0);
        ParkingSession completedSession = createCompletedSession(mike, vehicle5, spot3, new BigDecimal("1500.00"));
        
        // Create payment for completed session
        Payment payment = createPayment(completedSession, completedSession.getTotalAmount(), Payment.PaymentMethod.CARD);
        
        // Create pricing rules
        createPricingRules();
        
        System.out.println("✅ Database seeded successfully!");
        System.out.println("📊 Created:");
        System.out.println("   👥 Users: " + userRepository.count());
        System.out.println("   🚗 Vehicles: " + vehicleRepository.count());
        System.out.println("   🏢 Facilities: " + facilityRepository.count());
        System.out.println("   🅿️ Parking Spots: " + spotRepository.count());
        System.out.println("   📋 Sessions: " + sessionRepository.count());
        System.out.println("   💳 Payments: " + paymentRepository.count());
    }
    
    private User createUser(String username, String email, String firstName, String lastName, User.UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"); // password
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPhoneNumber("+255712345" + String.format("%03d", (int)(Math.random() * 999)));
        return userRepository.save(user);
    }
    
    private Vehicle createVehicle(User user, String licensePlate, String make, String model, String color, Vehicle.VehicleType type) {
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setColor(color);
        vehicle.setVehicleType(type);
        return vehicleRepository.save(vehicle);
    }
    
    private ParkingFacility createFacility(String name, ParkingFacility.FacilityType type, String address, 
                                          BigDecimal lat, BigDecimal lng, BigDecimal rate, int totalSpots) {
        ParkingFacility facility = new ParkingFacility();
        facility.setName(name);
        facility.setFacilityType(type);
        facility.setAddress(address);
        facility.setLocationLat(lat);
        facility.setLocationLng(lng);
        facility.setBaseHourlyRate(rate);
        facility.setTotalSpots(totalSpots);
        facility.setAvailableSpots(totalSpots);
        facility.setOperatingHoursStart(LocalTime.of(6, 0));
        facility.setOperatingHoursEnd(LocalTime.of(22, 0));
        return facilityRepository.save(facility);
    }
    
    private void createSpotsForFacility(ParkingFacility facility, int spotCount) {
        List<ParkingSpot> spots = new ArrayList<>();
        
        for (int i = 1; i <= spotCount; i++) {
            ParkingSpot spot = new ParkingSpot();
            spot.setFacility(facility);
            spot.setSpotNumber(String.format("%s%03d", facility.getFacilityType() == ParkingFacility.FacilityType.GARAGE ? "G" : "S", i));
            
            // Set spot types with realistic distribution
            if (i <= 5) {
                spot.setSpotType(ParkingSpot.SpotType.DISABLED);
            } else if (i <= 15) {
                spot.setSpotType(ParkingSpot.SpotType.ELECTRIC);
            } else if (i <= 50) {
                spot.setSpotType(ParkingSpot.SpotType.COMPACT);
            } else {
                spot.setSpotType(ParkingSpot.SpotType.REGULAR);
            }
            
            spot.setFloorLevel((i - 1) / 50); // 50 spots per floor
            spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
            spots.add(spot);
        }
        
        spotRepository.saveAll(spots);
    }
    
    private ParkingSession createActiveSession(User user, Vehicle vehicle, ParkingSpot spot, BigDecimal hourlyRate) {
        ParkingSession session = new ParkingSession();
        session.setUser(user);
        session.setVehicle(vehicle);
        session.setSpot(spot);
        session.setHourlyRate(hourlyRate);
        session.setStatus(ParkingSession.SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now().minusMinutes(30));
        session.setPlannedDurationHours(2);
        
        // Update spot status
        spot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        spotRepository.save(spot);
        
        return sessionRepository.save(session);
    }
    
    private ParkingSession createCompletedSession(User user, Vehicle vehicle, ParkingSpot spot, BigDecimal hourlyRate) {
        ParkingSession session = new ParkingSession();
        session.setUser(user);
        session.setVehicle(vehicle);
        session.setSpot(spot);
        session.setHourlyRate(hourlyRate);
        session.setStatus(ParkingSession.SessionStatus.COMPLETED);
        session.setStartedAt(LocalDateTime.now().minusHours(3));
        session.setEndedAt(LocalDateTime.now().minusMinutes(30));
        session.setActualDurationMinutes(150); // 2.5 hours
        session.setTotalAmount(hourlyRate.multiply(new BigDecimal("3"))); // Round up to 3 hours
        session.setPlannedDurationHours(2);
        
        return sessionRepository.save(session);
    }
    
    private Payment createPayment(ParkingSession session, BigDecimal amount, Payment.PaymentMethod method) {
        Payment payment = new Payment();
        payment.setSession(session);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now().minusMinutes(25));
        payment.setPaymentProvider("X-PAYMENT-PROVIDER");
        payment.setExternalPaymentId("XPP-" + System.currentTimeMillis());
        return paymentRepository.save(payment);
    }
    
    private void createPricingRules() {
        // Peak hour pricing
        PricingRule morningRush = new PricingRule();
        morningRush.setRuleName("Morning Rush Hour");
        morningRush.setRuleType(PricingRule.RuleType.TIME_BASED);
        morningRush.setPriority(100);
        morningRush.setStartTime(LocalTime.of(7, 0));
        morningRush.setEndTime(LocalTime.of(9, 0));
        morningRush.setMultiplier(new BigDecimal("1.50"));
        pricingRuleRepository.save(morningRush);
        
        PricingRule eveningRush = new PricingRule();
        eveningRush.setRuleName("Evening Rush Hour");
        eveningRush.setRuleType(PricingRule.RuleType.TIME_BASED);
        eveningRush.setPriority(100);
        eveningRush.setStartTime(LocalTime.of(17, 0));
        eveningRush.setEndTime(LocalTime.of(19, 0));
        eveningRush.setMultiplier(new BigDecimal("1.50"));
        pricingRuleRepository.save(eveningRush);
        
        // High demand pricing
        PricingRule highDemand = new PricingRule();
        highDemand.setRuleName("High Demand Surge");
        highDemand.setRuleType(PricingRule.RuleType.DEMAND_BASED);
        highDemand.setPriority(200);
        highDemand.setDemandThresholdPercentage(new BigDecimal("85.00"));
        highDemand.setMultiplier(new BigDecimal("2.00"));
        pricingRuleRepository.save(highDemand);
    }
}