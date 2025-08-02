package com.evmak.parking_management.controller;

import com.evmak.parking_management.service.DataSeedingService;
import com.evmak.parking_management.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data Management", description = "Database seeding and management operations")
public class DataController {

    @Autowired
    private DataSeedingService dataSeedingService;
    
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

    @PostMapping("/seed")
    @Operation(summary = "Seed database", description = "Populate database with basic sample data")
    public ResponseEntity<Map<String, Object>> seedDatabase() {
        try {
            dataSeedingService.seedData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database seeded successfully with basic sample data");
            response.put("data", getDataStatistics());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error seeding database: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/seed/large")
    @Operation(summary = "Seed large dataset", description = "Populate database with large-scale test data (1000+ spots, 10000+ sessions)")
    public ResponseEntity<Map<String, Object>> seedLargeDataset() {
        try {
            long startTime = System.currentTimeMillis();
            dataSeedingService.seedLargeDataset();
            long endTime = System.currentTimeMillis();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Large-scale dataset seeded successfully");
            response.put("executionTimeMs", endTime - startTime);
            response.put("data", getDataStatistics());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error seeding large dataset: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get data statistics", description = "Get current database statistics")
    public ResponseEntity<Map<String, Object>> getDataStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userRepository.count());
        stats.put("vehicles", vehicleRepository.count());
        stats.put("facilities", facilityRepository.count());
        stats.put("parkingSpots", spotRepository.count());
        stats.put("parkingSessions", sessionRepository.count());
        stats.put("payments", paymentRepository.count());
        
        // Calculate some interesting metrics
        long totalCapacity = facilityRepository.findAll().stream()
            .mapToLong(f -> f.getTotalSpots())
            .sum();
        
        long totalAvailable = facilityRepository.findAll().stream()
            .mapToLong(f -> f.getAvailableSpots())
            .sum();
        
        double occupancyRate = totalCapacity > 0 ? 
            ((double)(totalCapacity - totalAvailable) / totalCapacity) * 100 : 0;
        
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalAvailable", totalAvailable);
        stats.put("occupancyRate", String.format("%.1f%%", occupancyRate));
        
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/clear")
    @Operation(summary = "Clear all data", description = "WARNING: Delete all data from database")
    public ResponseEntity<Map<String, Object>> clearAllData() {
        try {
            paymentRepository.deleteAll();
            sessionRepository.deleteAll();
            spotRepository.deleteAll();
            vehicleRepository.deleteAll();
            facilityRepository.deleteAll();
            userRepository.deleteAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All data cleared successfully");
            response.put("data", getDataStatistics());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error clearing data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}