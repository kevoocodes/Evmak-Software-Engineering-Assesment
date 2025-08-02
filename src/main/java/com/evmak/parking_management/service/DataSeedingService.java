package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.*;
import com.evmak.parking_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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

    @Transactional
    public void seedData() {
        seedDatabase();
    }
    
    @Transactional
    public void seedLargeDataset() {
        System.out.println("🚀 Starting LARGE SCALE data seeding...");
        System.out.println("📊 Target: 1000+ parking spots, 10,000+ sessions");
        
        // Clear existing data first
        clearExistingData();
        
        // Create comprehensive dataset
        List<User> users = createLargeUserBase(500); // 500 users
        List<Vehicle> vehicles = createLargeVehicleBase(users, 1200); // 1200 vehicles
        List<ParkingFacility> facilities = createLargeFacilityBase(25); // 25 facilities
        List<ParkingSpot> spots = createLargeParkingSpotBase(facilities, 1000); // 1000+ spots
        createLargeParkingSessionBase(users, vehicles, spots, 10000); // 10,000 sessions
        
        System.out.println("✅ LARGE SCALE data seeding completed!");
        printDataStatistics();
    }
    
    private void clearExistingData() {
        System.out.println("🧹 Clearing existing data...");
        paymentRepository.deleteAll();
        sessionRepository.deleteAll();
        spotRepository.deleteAll();
        vehicleRepository.deleteAll();
        facilityRepository.deleteAll();
        userRepository.deleteAll();
        pricingRuleRepository.deleteAll();
    }

    private void seedDatabase() {
        if (userRepository.count() > 0) {
            System.out.println("⚠️ Database already contains data. Skipping basic seeding.");
            return;
        }
        
        System.out.println("🌱 Seeding database with basic sample data...");
        
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
        
        // Create Parking Spots
        createSpotsForFacility(cityCenter, 200);
        createSpotsForFacility(businessDistrict, 180);
        createSpotsForFacility(shoppingMall, 300);
        
        // Create pricing rules
        createPricingRules();
        
        printDataStatistics();
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
    
    // ========== LARGE SCALE DATA GENERATION ==========
    
    private List<User> createLargeUserBase(int userCount) {
        System.out.println("👥 Creating " + userCount + " users...");
        List<User> users = new ArrayList<>();
        Random random = new Random();
        
        String[] firstNames = {"John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Jessica", "William", "Ashley",
                              "James", "Amanda", "Christopher", "Stephanie", "Daniel", "Melissa", "Matthew", "Nicole", "Anthony", "Elizabeth",
                              "Mark", "Heather", "Donald", "Tiffany", "Steven", "Michelle", "Paul", "Angela", "Andrew", "Kimberly",
                              "Joshua", "Amy", "Kenneth", "Mary", "Kevin", "Lisa", "Brian", "Nancy", "George", "Karen",
                              "Joseph", "Betty", "Thomas", "Helen", "Charles", "Sandra", "Ryan", "Donna", "Jason", "Carol"};
        
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
                              "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
                              "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
                              "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
                              "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts"};
        
        // Create admin users
        for (int i = 0; i < 5; i++) {
            User admin = new User();
            admin.setUsername("admin" + (i + 1));
            admin.setEmail("admin" + (i + 1) + "@parking.com");
            admin.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
            admin.setFirstName("Admin");
            admin.setLastName("User" + (i + 1));
            admin.setRole(User.UserRole.ADMIN);
            admin.setPhoneNumber("+25571234" + String.format("%04d", i));
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            admin.setUpdatedAt(admin.getCreatedAt());
            users.add(admin);
        }
        
        // Create parking attendants
        for (int i = 0; i < 20; i++) {
            User attendant = new User();
            attendant.setUsername("attendant" + (i + 1));
            attendant.setEmail("attendant" + (i + 1) + "@parking.com");
            attendant.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
            attendant.setFirstName(firstNames[random.nextInt(firstNames.length)]);
            attendant.setLastName(lastNames[random.nextInt(lastNames.length)]);
            attendant.setRole(User.UserRole.PARKING_ATTENDANT);
            attendant.setPhoneNumber("+25572" + String.format("%07d", random.nextInt(10000000)));
            attendant.setIsActive(true);
            attendant.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            attendant.setUpdatedAt(attendant.getCreatedAt());
            users.add(attendant);
        }
        
        // Create regular users
        for (int i = 0; i < userCount - 25; i++) {
            User user = new User();
            user.setUsername("user" + (i + 1));
            user.setEmail("user" + (i + 1) + "@example.com");
            user.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
            user.setFirstName(firstNames[random.nextInt(firstNames.length)]);
            user.setLastName(lastNames[random.nextInt(lastNames.length)]);
            user.setRole(User.UserRole.USER);
            user.setPhoneNumber("+25570" + String.format("%07d", random.nextInt(10000000)));
            user.setIsActive(random.nextBoolean() ? true : random.nextDouble() > 0.1); // 90% active
            user.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            user.setUpdatedAt(user.getCreatedAt().plusDays(random.nextInt(30)));
            users.add(user);
        }
        
        return userRepository.saveAll(users);
    }
    
    private List<Vehicle> createLargeVehicleBase(List<User> users, int vehicleCount) {
        System.out.println("🚗 Creating " + vehicleCount + " vehicles...");
        List<Vehicle> vehicles = new ArrayList<>();
        Random random = new Random();
        
        String[] makes = {"Toyota", "Honda", "Nissan", "Ford", "BMW", "Mercedes", "Audi", "Volkswagen", "Hyundai", "Kia",
                         "Mazda", "Subaru", "Mitsubishi", "Isuzu", "Suzuki", "Daihatsu", "Peugeot", "Renault", "Volvo", "Land Rover"};
        
        String[] models = {"Corolla", "Camry", "Prius", "RAV4", "Hilux", "Civic", "Accord", "CR-V", "Pilot", "Fit",
                          "Altima", "Sentra", "X-Trail", "Patrol", "Hardbody", "Focus", "Ranger", "Explorer", "F-150", "Mustang",
                          "320i", "X3", "X5", "325i", "523i", "C-Class", "E-Class", "GLC", "ML", "S-Class"};
        
        String[] colors = {"White", "Black", "Silver", "Red", "Blue", "Gray", "Green", "Yellow", "Orange", "Brown"};
        
        Vehicle.VehicleType[] types = Vehicle.VehicleType.values();
        
        for (int i = 0; i < vehicleCount; i++) {
            Vehicle vehicle = new Vehicle();
            
            // Assign to random user (some users can have multiple vehicles)
            User randomUser = users.get(random.nextInt(users.size()));
            vehicle.setUser(randomUser);
            
            // Generate license plate (Tanzanian format)
            String plate = "T" + String.format("%03d", random.nextInt(1000)) + 
                          (char)('A' + random.nextInt(26)) + 
                          (char)('A' + random.nextInt(26)) + 
                          (char)('A' + random.nextInt(26));
            vehicle.setLicensePlate(plate);
            
            vehicle.setMake(makes[random.nextInt(makes.length)]);
            vehicle.setModel(models[random.nextInt(models.length)]);
            vehicle.setColor(colors[random.nextInt(colors.length)]);
            vehicle.setVehicleType(types[random.nextInt(types.length)]);
            vehicle.setIsActive(random.nextDouble() > 0.05); // 95% active
            vehicle.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            vehicle.setUpdatedAt(vehicle.getCreatedAt().plusDays(random.nextInt(30)));
            
            vehicles.add(vehicle);
        }
        
        return vehicleRepository.saveAll(vehicles);
    }
    
    private List<ParkingFacility> createLargeFacilityBase(int facilityCount) {
        System.out.println("🏢 Creating " + facilityCount + " parking facilities...");
        List<ParkingFacility> facilities = new ArrayList<>();
        Random random = new Random();
        
        String[] facilityNames = {
            "Central Business District Garage", "Kivukoni Front Parking", "Uhuru Street Complex", "Samora Avenue Garage",
            "Msimbazi Center Parking", "Kariakoo Market Garage", "Mchafukoge Street Zone", "India Street Parking",
            "Ocean Road Complex", "Bibi Titi Street Garage", "Mnazi Mmoja Grounds Parking", "Azikiwe Street Zone",
            "Julius Nyerere Airport Terminal 1", "Julius Nyerere Airport Terminal 2", "Airport Cargo Complex",
            "Mlimani City Mall Garage", "Quality Center Mall Parking", "Slipway Shopping Center", "Sea Cliff Village",
            "Peninsula Hotel Complex", "Hyatt Regency Garage", "Kilimanjaro Hotel Parking", "Four Points Sheraton",
            "University of Dar es Salaam", "Muhimbili Hospital Complex"
        };
        
        String[] streets = {
            "Uhuru Street", "Samora Avenue", "Kivukoni Front", "Ocean Road", "India Street", "Bibi Titi Mohamed Street",
            "Msimbazi Street", "Mchafukoge Street", "Azikiwe Street", "Morogoro Road", "Ali Hassan Mwinyi Road",
            "Bagamoyo Road", "Nelson Mandela Road", "Julius Nyerere Road", "Kilwa Road"
        };
        
        // Base coordinates for Dar es Salaam
        double baseLat = -6.7924;
        double baseLng = 39.2083;
        
        for (int i = 0; i < facilityCount; i++) {
            ParkingFacility facility = new ParkingFacility();
            
            facility.setName(facilityNames[i % facilityNames.length] + (i >= facilityNames.length ? " " + (i / facilityNames.length + 1) : ""));
            
            // Mix of garage and street zone types
            facility.setFacilityType(random.nextDouble() > 0.3 ? 
                ParkingFacility.FacilityType.GARAGE : ParkingFacility.FacilityType.STREET_ZONE);
            
            facility.setAddress((random.nextInt(999) + 1) + " " + streets[random.nextInt(streets.length)] + ", Dar es Salaam");
            
            // Generate coordinates within Dar es Salaam area
            facility.setLocationLat(new BigDecimal(baseLat + (random.nextGaussian() * 0.05)));
            facility.setLocationLng(new BigDecimal(baseLng + (random.nextGaussian() * 0.05)));
            
            // Realistic pricing based on facility type and location
            BigDecimal baseRate;
            if (facility.getFacilityType() == ParkingFacility.FacilityType.GARAGE) {
                baseRate = new BigDecimal(1500 + random.nextInt(3500)); // 1500-5000 TZS
            } else {
                baseRate = new BigDecimal(1000 + random.nextInt(2000)); // 1000-3000 TZS
            }
            facility.setBaseHourlyRate(baseRate);
            
            // Realistic spot counts
            int totalSpots;
            if (facility.getFacilityType() == ParkingFacility.FacilityType.GARAGE) {
                totalSpots = 50 + random.nextInt(450); // 50-500 spots
            } else {
                totalSpots = 10 + random.nextInt(40); // 10-50 spots
            }
            facility.setTotalSpots(totalSpots);
            facility.setAvailableSpots((int)(totalSpots * (0.6 + random.nextDouble() * 0.4))); // 60-100% available
            
            facility.setMaxHours(random.nextInt(20) + 4); // 4-24 hours max
            facility.setIsActive(random.nextDouble() > 0.05); // 95% active
            
            // Operating hours
            int startHour = random.nextInt(3) + 5; // 5-7 AM
            int endHour = random.nextInt(4) + 20; // 8-11 PM
            facility.setOperatingHoursStart(LocalTime.of(startHour, 0));
            facility.setOperatingHoursEnd(LocalTime.of(endHour, 0));
            
            facility.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(730))); // Up to 2 years old
            facility.setUpdatedAt(facility.getCreatedAt().plusDays(random.nextInt(30)));
            
            facilities.add(facility);
        }
        
        return facilityRepository.saveAll(facilities);
    }
    
    private List<ParkingSpot> createLargeParkingSpotBase(List<ParkingFacility> facilities, int minTotalSpots) {
        System.out.println("🅿️ Creating " + minTotalSpots + "+ parking spots across " + facilities.size() + " facilities...");
        List<ParkingSpot> allSpots = new ArrayList<>();
        Random random = new Random();
        
        int totalSpotsCreated = 0;
        
        for (ParkingFacility facility : facilities) {
            int spotsForFacility = facility.getTotalSpots();
            List<ParkingSpot> spots = new ArrayList<>();
            
            for (int i = 1; i <= spotsForFacility; i++) {
                ParkingSpot spot = new ParkingSpot();
                spot.setFacility(facility);
                
                // Generate spot number based on facility type
                if (facility.getFacilityType() == ParkingFacility.FacilityType.GARAGE) {
                    int floor = (i - 1) / 50; // 50 spots per floor
                    int spotOnFloor = ((i - 1) % 50) + 1;
                    spot.setSpotNumber(String.format("L%d-%03d", floor, spotOnFloor));
                    spot.setFloorLevel(floor);
                } else {
                    spot.setSpotNumber(String.format("S%03d", i));
                    spot.setFloorLevel(0); // Street level
                }
                
                // Realistic spot type distribution
                double rand = random.nextDouble();
                if (rand < 0.05) {
                    spot.setSpotType(ParkingSpot.SpotType.DISABLED);
                } else if (rand < 0.15) {
                    spot.setSpotType(ParkingSpot.SpotType.ELECTRIC);
                } else if (rand < 0.35) {
                    spot.setSpotType(ParkingSpot.SpotType.COMPACT);
                } else {
                    spot.setSpotType(ParkingSpot.SpotType.REGULAR);
                }
                
                // Status distribution (most available, some occupied/reserved)
                double statusRand = random.nextDouble();
                if (statusRand < 0.70) {
                    spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
                } else if (statusRand < 0.85) {
                    spot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
                } else if (statusRand < 0.95) {
                    spot.setStatus(ParkingSpot.SpotStatus.RESERVED);
                    spot.setReservationExpiresAt(LocalDateTime.now().plusMinutes(random.nextInt(60)));
                } else {
                    spot.setStatus(ParkingSpot.SpotStatus.OUT_OF_ORDER);
                }
                
                spot.setCreatedAt(facility.getCreatedAt().plusDays(random.nextInt(7)));
                spot.setLastUpdated(LocalDateTime.now().minusMinutes(random.nextInt(1440))); // Updated in last 24h
                
                spots.add(spot);
                totalSpotsCreated++;
            }
            
            spotRepository.saveAll(spots);
            allSpots.addAll(spots);
        }
        
        // If we haven't reached minimum spots, create additional large facilities
        if (totalSpotsCreated < minTotalSpots) {
            int additionalSpotsNeeded = minTotalSpots - totalSpotsCreated;
            System.out.println("📈 Creating additional facilities for " + additionalSpotsNeeded + " more spots...");
            
            // Create mega facilities
            List<ParkingFacility> megaFacilities = createMegaFacilities(additionalSpotsNeeded);
            for (ParkingFacility megaFacility : megaFacilities) {
                List<ParkingSpot> megaSpots = createSpotsForMegaFacility(megaFacility);
                allSpots.addAll(megaSpots);
                totalSpotsCreated += megaSpots.size();
            }
        }
        
        System.out.println("✅ Created " + totalSpotsCreated + " parking spots total");
        return allSpots;
    }
    
    private List<ParkingFacility> createMegaFacilities(int additionalSpotsNeeded) {
        List<ParkingFacility> megaFacilities = new ArrayList<>();
        Random random = new Random();
        
        String[] megaNames = {
            "Dar es Salaam International Convention Centre", "Mlimani City Mega Complex", 
            "CBD Central Mega Garage", "Airport Long-term Parking", "Port Authority Mega Lot",
            "University Mega Campus Parking", "Stadium Mega Complex", "Government Complex Mega Garage"
        };
        
        int facilitiesNeeded = Math.max(1, additionalSpotsNeeded / 400); // ~400 spots per mega facility
        
        for (int i = 0; i < facilitiesNeeded; i++) {
            ParkingFacility megaFacility = new ParkingFacility();
            megaFacility.setName(megaNames[i % megaNames.length] + (i >= megaNames.length ? " " + (i / megaNames.length + 1) : ""));
            megaFacility.setFacilityType(ParkingFacility.FacilityType.GARAGE);
            megaFacility.setAddress("Mega Complex " + (i + 1) + ", Dar es Salaam");
            megaFacility.setLocationLat(new BigDecimal(-6.7924 + (random.nextGaussian() * 0.03)));
            megaFacility.setLocationLng(new BigDecimal(39.2083 + (random.nextGaussian() * 0.03)));
            megaFacility.setBaseHourlyRate(new BigDecimal(2000 + random.nextInt(2000)));
            
            int spotsForThis = Math.min(500, (additionalSpotsNeeded / facilitiesNeeded) + (i == facilitiesNeeded - 1 ? additionalSpotsNeeded % facilitiesNeeded : 0));
            megaFacility.setTotalSpots(spotsForThis);
            megaFacility.setAvailableSpots((int)(spotsForThis * (0.7 + random.nextDouble() * 0.3)));
            megaFacility.setMaxHours(24);
            megaFacility.setIsActive(true);
            megaFacility.setOperatingHoursStart(LocalTime.of(0, 0));
            megaFacility.setOperatingHoursEnd(LocalTime.of(23, 59));
            megaFacility.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            megaFacility.setUpdatedAt(megaFacility.getCreatedAt());
            
            megaFacilities.add(facilityRepository.save(megaFacility));
        }
        
        return megaFacilities;
    }
    
    private List<ParkingSpot> createSpotsForMegaFacility(ParkingFacility facility) {
        List<ParkingSpot> spots = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 1; i <= facility.getTotalSpots(); i++) {
            ParkingSpot spot = new ParkingSpot();
            spot.setFacility(facility);
            
            int floor = (i - 1) / 100; // 100 spots per floor for mega facilities
            int spotOnFloor = ((i - 1) % 100) + 1;
            spot.setSpotNumber(String.format("M%d-%03d", floor, spotOnFloor));
            spot.setFloorLevel(floor);
            
            // Spot type distribution
            double rand = random.nextDouble();
            if (rand < 0.03) {
                spot.setSpotType(ParkingSpot.SpotType.DISABLED);
            } else if (rand < 0.20) {
                spot.setSpotType(ParkingSpot.SpotType.ELECTRIC);
            } else if (rand < 0.40) {
                spot.setSpotType(ParkingSpot.SpotType.COMPACT);
            } else {
                spot.setSpotType(ParkingSpot.SpotType.REGULAR);
            }
            
            spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
            spot.setCreatedAt(facility.getCreatedAt());
            spot.setLastUpdated(LocalDateTime.now());
            
            spots.add(spot);
        }
        
        return spotRepository.saveAll(spots);
    }
    
    private void createLargeParkingSessionBase(List<User> users, List<Vehicle> vehicles, List<ParkingSpot> spots, int sessionCount) {
        System.out.println("📋 Creating " + sessionCount + " parking sessions...");
        
        Random random = new Random();
        List<ParkingSession> sessions = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();
        
        // Only use regular users for sessions (exclude admins)
        List<User> regularUsers = users.stream()
            .filter(u -> u.getRole() == User.UserRole.USER)
            .toList();
        
        ParkingSession.SessionStatus[] statuses = ParkingSession.SessionStatus.values();
        Payment.PaymentMethod[] paymentMethods = Payment.PaymentMethod.values();
        Payment.PaymentStatus[] paymentStatuses = Payment.PaymentStatus.values();
        
        int batchSize = 1000;
        for (int batch = 0; batch < sessionCount; batch += batchSize) {
            int endBatch = Math.min(batch + batchSize, sessionCount);
            List<ParkingSession> batchSessions = new ArrayList<>();
            List<Payment> batchPayments = new ArrayList<>();
            
            for (int i = batch; i < endBatch; i++) {
                try {
                    ParkingSession session = new ParkingSession();
                    
                    // Random user and their vehicle
                    User randomUser = regularUsers.get(random.nextInt(regularUsers.size()));
                    List<Vehicle> userVehicles = vehicles.stream()
                        .filter(v -> v.getUser().getId().equals(randomUser.getId()))
                        .toList();
                    
                    if (userVehicles.isEmpty()) {
                        continue; // Skip if user has no vehicles
                    }
                    
                    Vehicle randomVehicle = userVehicles.get(random.nextInt(userVehicles.size()));
                    ParkingSpot randomSpot = spots.get(random.nextInt(spots.size()));
                    
                    session.setUser(randomUser);
                    session.setVehicle(randomVehicle);
                    session.setSpot(randomSpot);
                    session.setHourlyRate(randomSpot.getFacility().getBaseHourlyRate());
                    
                    // Generate realistic session reference
                    session.setSessionReference("SES-" + String.format("%09d", i + 1));
                    
                    // Random session status distribution
                    double statusRand = random.nextDouble();
                    ParkingSession.SessionStatus status;
                    if (statusRand < 0.75) {
                        status = ParkingSession.SessionStatus.COMPLETED;
                    } else if (statusRand < 0.90) {
                        status = ParkingSession.SessionStatus.ACTIVE;
                    } else if (statusRand < 0.95) {
                        status = ParkingSession.SessionStatus.EXPIRED;
                    } else {
                        status = ParkingSession.SessionStatus.CANCELLED;
                    }
                    session.setStatus(status);
                    
                    // Generate realistic timestamps
                    LocalDateTime baseTime = LocalDateTime.now()
                        .minusDays(random.nextInt(90)) // Last 90 days
                        .withHour(random.nextInt(16) + 6) // 6 AM to 10 PM
                        .withMinute(random.nextInt(60))
                        .withSecond(0)
                        .withNano(0);
                    
                    session.setStartedAt(baseTime);
                    
                    int plannedHours = random.nextInt(8) + 1; // 1-8 hours
                    session.setPlannedDurationHours(plannedHours);
                    
                    if (status == ParkingSession.SessionStatus.COMPLETED || 
                        status == ParkingSession.SessionStatus.EXPIRED) {
                        
                        int actualMinutes = (int)(plannedHours * 60 * (0.8 + random.nextDouble() * 0.4)); // 80-120% of planned
                        session.setActualDurationMinutes(actualMinutes);
                        session.setEndedAt(baseTime.plusMinutes(actualMinutes));
                        
                        // Calculate total amount
                        double hours = Math.ceil(actualMinutes / 60.0);
                        BigDecimal totalAmount = session.getHourlyRate().multiply(new BigDecimal(hours));
                        session.setTotalAmount(totalAmount);
                        
                        // Create payment for completed sessions
                        if (status == ParkingSession.SessionStatus.COMPLETED && random.nextDouble() > 0.05) { // 95% paid
                            Payment payment = new Payment();
                            payment.setAmount(totalAmount);
                            payment.setCurrency("TZS");
                            payment.setPaymentMethod(paymentMethods[random.nextInt(paymentMethods.length)]);
                            payment.setPaymentProvider("X-PAYMENT-PROVIDER");
                            payment.setPaymentReference("PAY-" + String.format("%09d", i + 1));
                            payment.setExternalPaymentId("XPP-" + System.currentTimeMillis() + "-" + i);
                            
                            // Payment status distribution
                            double paymentRand = random.nextDouble();
                            if (paymentRand < 0.90) {
                                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                                payment.setCompletedAt(session.getEndedAt().plusMinutes(random.nextInt(30)));
                            } else if (paymentRand < 0.95) {
                                payment.setStatus(Payment.PaymentStatus.PENDING);
                            } else {
                                payment.setStatus(Payment.PaymentStatus.FAILED);
                            }
                            
                            payment.setCreatedAt(session.getEndedAt());
                            payment.setUpdatedAt(payment.getCreatedAt());
                            
                            batchPayments.add(payment);
                            payment.setSession(session); // Will be set after session is saved
                        }
                    } else if (status == ParkingSession.SessionStatus.ACTIVE) {
                        // Active sessions - no end time yet
                        session.setActualDurationMinutes(null);
                        session.setEndedAt(null);
                        session.setTotalAmount(null);
                    }
                    
                    session.setCreatedAt(baseTime.minusMinutes(random.nextInt(30)));
                    session.setUpdatedAt(session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now());
                    
                    batchSessions.add(session);
                    
                } catch (Exception e) {
                    System.err.println("Error creating session " + i + ": " + e.getMessage());
                }
            }
            
            // Save batch
            List<ParkingSession> savedSessions = sessionRepository.saveAll(batchSessions);
            
            // Update payment sessions and save payments
            for (int j = 0; j < batchPayments.size() && j < savedSessions.size(); j++) {
                batchPayments.get(j).setSession(savedSessions.get(j));
            }
            
            if (!batchPayments.isEmpty()) {
                paymentRepository.saveAll(batchPayments);
            }
            
            System.out.println("✅ Completed batch " + (batch / batchSize + 1) + " (" + endBatch + "/" + sessionCount + " sessions)");
        }
        
        System.out.println("✅ Created " + sessionCount + " parking sessions with payments");
    }
    
    private void printDataStatistics() {
        System.out.println("\n📊 DATABASE STATISTICS:");
        System.out.println("═══════════════════════");
        System.out.println("👥 Users: " + userRepository.count());
        System.out.println("🚗 Vehicles: " + vehicleRepository.count());
        System.out.println("🏢 Facilities: " + facilityRepository.count());
        System.out.println("🅿️ Parking Spots: " + spotRepository.count());
        System.out.println("📋 Parking Sessions: " + sessionRepository.count());
        System.out.println("💳 Payments: " + paymentRepository.count());
        System.out.println("💰 Pricing Rules: " + pricingRuleRepository.count());
        System.out.println("═══════════════════════\n");
    }
}