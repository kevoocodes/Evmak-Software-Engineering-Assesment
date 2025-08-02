package com.evmak.parking_management.controller;

import com.evmak.parking_management.service.DataSeedingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data Management", description = "Database seeding and management operations")
public class DataController {

    @Autowired
    private DataSeedingService dataSeedingService;

    @PostMapping("/seed")
    @Operation(summary = "Seed database", description = "Populate database with sample data")
    public ResponseEntity<String> seedDatabase() {
        try {
            dataSeedingService.seedData();
            return ResponseEntity.ok("Database seeded successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error seeding database: " + e.getMessage());
        }
    }
}