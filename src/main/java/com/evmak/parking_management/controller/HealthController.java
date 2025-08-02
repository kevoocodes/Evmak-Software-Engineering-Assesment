package com.evmak.parking_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("application", "Parking Management System");
        status.put("timestamp", System.currentTimeMillis());
        
        // Test database connection
        try (Connection connection = dataSource.getConnection()) {
            status.put("database", "Connected");
            status.put("databaseUrl", connection.getMetaData().getURL());
        } catch (Exception e) {
            status.put("database", "Failed: " + e.getMessage());
            status.put("status", "DOWN");
        }
        
        return ResponseEntity.ok(status);
    }
}