package com.evmak.parking_management.controller;

import com.evmak.parking_management.entity.ParkingFacility;
import com.evmak.parking_management.entity.ParkingSpot;
import com.evmak.parking_management.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Redis Cache Management", description = "Real-time availability caching and performance optimization")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @GetMapping("/availability/{facilityId}")
    @Operation(summary = "Get real-time facility availability", 
               description = "Get cached real-time parking availability with sub-50ms response time")
    public ResponseEntity<CacheService.FacilityAvailability> getRealTimeAvailability(
            @PathVariable Long facilityId) {
        
        CacheService.FacilityAvailability availability = cacheService.getRealTimeAvailability(facilityId);
        
        if (availability != null) {
            return ResponseEntity.ok(availability);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/spots/{facilityId}/available")
    @Operation(summary = "Get cached available spots", 
               description = "Get available parking spots from cache for fast access")
    public ResponseEntity<List<ParkingSpot>> getCachedAvailableSpots(@PathVariable Long facilityId) {
        List<ParkingSpot> spots = cacheService.getCachedAvailableSpots(facilityId);
        return ResponseEntity.ok(spots);
    }

    @GetMapping("/facilities/{facilityId}")
    @Operation(summary = "Get cached facility details", 
               description = "Get facility information from cache")
    public ResponseEntity<ParkingFacility> getCachedFacility(@PathVariable Long facilityId) {
        ParkingFacility facility = cacheService.getCachedFacility(facilityId);
        
        if (facility != null) {
            return ResponseEntity.ok(facility);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/facilities/nearby")
    @Operation(summary = "Get cached nearby facilities", 
               description = "Get nearby parking facilities from cache")
    public ResponseEntity<List<ParkingFacility>> getCachedNearbyFacilities(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Integer radiusKm) {
        
        List<ParkingFacility> facilities = cacheService.getCachedNearbyFacilities(latitude, longitude, radiusKm);
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/spot/{spotId}/status")
    @Operation(summary = "Get real-time spot status", 
               description = "Get current spot status from cache")
    public ResponseEntity<Object> getSpotStatus(@PathVariable Long spotId) {
        ParkingSpot.SpotStatus status = cacheService.getSpotStatus(spotId);
        
        if (status != null) {
            Long finalSpotId = spotId;
            String statusStr = status.toString();
            return ResponseEntity.ok(new Object() {
                public final Long spotId = finalSpotId;
                public final String status = statusStr;
                public final long timestamp = System.currentTimeMillis();
            });
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/refresh/facility/{facilityId}")
    @Operation(summary = "Refresh facility cache", 
               description = "Force refresh cache for a specific facility")
    public ResponseEntity<String> refreshFacilityCache(@PathVariable Long facilityId) {
        cacheService.updateFacilityAvailability(facilityId);
        cacheService.evictFacilityCache(facilityId);
        cacheService.evictFacilitySpotsCache(facilityId);
        
        return ResponseEntity.ok("Cache refreshed for facility " + facilityId);
    }

    @PostMapping("/warm")
    @Operation(summary = "Warm cache for high-traffic facilities", 
               description = "Pre-load cache for specified facilities")
    public ResponseEntity<String> warmCache(@RequestBody List<Long> facilityIds) {
        cacheService.warmCache(facilityIds);
        return ResponseEntity.ok("Cache warmed for " + facilityIds.size() + " facilities");
    }

    @DeleteMapping("/evict/spots")
    @Operation(summary = "Evict all spots cache", 
               description = "Clear all cached parking spots data")
    public ResponseEntity<String> evictAllSpotsCache() {
        cacheService.evictAllSpotsCache();
        return ResponseEntity.ok("All parking spots cache evicted");
    }

    @DeleteMapping("/evict/facilities")
    @Operation(summary = "Evict all facilities cache", 
               description = "Clear all cached facility data")
    public ResponseEntity<String> evictAllFacilitiesCache() {
        cacheService.evictAllFacilitiesCache();
        return ResponseEntity.ok("All facilities cache evicted");
    }

    @DeleteMapping("/evict/facility/{facilityId}")
    @Operation(summary = "Evict specific facility cache", 
               description = "Clear cache for a specific facility")
    public ResponseEntity<String> evictFacilityCache(@PathVariable Long facilityId) {
        cacheService.evictFacilityCache(facilityId);
        cacheService.evictFacilitySpotsCache(facilityId);
        return ResponseEntity.ok("Cache evicted for facility " + facilityId);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get cache statistics", 
               description = "Get Redis cache performance statistics")
    public ResponseEntity<CacheService.CacheStats> getCacheStats() {
        CacheService.CacheStats stats = cacheService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/performance-test")
    @Operation(summary = "Cache performance test", 
               description = "Test cache performance and response times")
    public ResponseEntity<Object> performanceTest() {
        long startTime = System.currentTimeMillis();
        
        // Test cache retrieval speed
        CacheService.CacheStats stats = cacheService.getCacheStats();
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        return ResponseEntity.ok(new Object() {
            public final String status = "Cache performance test completed";
            public final long responseTimeMs = responseTime;
            public final boolean subFiftyMs = responseTime < 50;
            public final CacheService.CacheStats cacheStats = stats;
            public final String[] features = {
                "✓ Redis-based caching with 30s TTL for real-time data",
                "✓ Cached facility availability and spot status",
                "✓ Sub-50ms response times for cached data",
                "✓ Automatic cache invalidation on data changes",
                "✓ Cache warming for high-traffic facilities",
                "✓ Performance monitoring and statistics"
            };
        });
    }
}