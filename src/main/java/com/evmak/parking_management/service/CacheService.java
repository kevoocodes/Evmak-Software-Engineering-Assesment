package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.ParkingSpot;
import com.evmak.parking_management.entity.ParkingFacility;
import com.evmak.parking_management.repository.ParkingSpotRepository;
import com.evmak.parking_management.repository.ParkingFacilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private ParkingFacilityRepository facilityRepository;

    private static final String AVAILABILITY_KEY_PREFIX = "availability:facility:";
    private static final String SPOT_STATUS_KEY_PREFIX = "spot:status:";
    private static final String FACILITY_STATS_KEY_PREFIX = "facility:stats:";

    // Real-time availability tracking
    public void updateSpotAvailability(Long facilityId, Long spotId, ParkingSpot.SpotStatus status) {
        String key = SPOT_STATUS_KEY_PREFIX + spotId;
        redisTemplate.opsForValue().set(key, status.toString(), 30, TimeUnit.SECONDS);
        
        // Update facility availability count
        updateFacilityAvailability(facilityId);
        
        // Invalidate cached spot data
        evictSpotCache(spotId);
    }

    public void updateFacilityAvailability(Long facilityId) {
        try {
            // Get real-time count from database
            Integer availableCount = spotRepository.countAvailableSpotsByFacilityId(facilityId);
            Integer totalCount = spotRepository.countSpotsByFacilityId(facilityId);
            
            // Store in Redis with short TTL for real-time access
            String availabilityKey = AVAILABILITY_KEY_PREFIX + facilityId;
            String statsKey = FACILITY_STATS_KEY_PREFIX + facilityId;
            
            FacilityAvailability availability = new FacilityAvailability(facilityId, availableCount, totalCount, LocalDateTime.now());
            
            redisTemplate.opsForValue().set(availabilityKey, availability, 30, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(statsKey, availability, 30, TimeUnit.SECONDS);
            
            // Invalidate cached facility data
            evictFacilityCache(facilityId);
            
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to update facility availability cache: " + e.getMessage());
        }
    }

    public FacilityAvailability getRealTimeAvailability(Long facilityId) {
        String key = AVAILABILITY_KEY_PREFIX + facilityId;
        FacilityAvailability cached = (FacilityAvailability) redisTemplate.opsForValue().get(key);
        
        if (cached != null && cached.isRecent()) {
            return cached;
        }
        
        // Cache miss or stale data - update and return fresh data
        updateFacilityAvailability(facilityId);
        return (FacilityAvailability) redisTemplate.opsForValue().get(key);
    }

    public ParkingSpot.SpotStatus getSpotStatus(Long spotId) {
        String key = SPOT_STATUS_KEY_PREFIX + spotId;
        String status = (String) redisTemplate.opsForValue().get(key);
        
        if (status != null) {
            return ParkingSpot.SpotStatus.valueOf(status);
        }
        
        // Cache miss - fetch from database and cache
        ParkingSpot spot = spotRepository.findById(spotId).orElse(null);
        if (spot != null) {
            redisTemplate.opsForValue().set(key, spot.getStatus().toString(), 30, TimeUnit.SECONDS);
            return spot.getStatus();
        }
        
        return null;
    }

    // Cached database queries
    @Cacheable(value = "parkingSpots", key = "#facilityId + ':available'")
    public List<ParkingSpot> getCachedAvailableSpots(Long facilityId) {
        return spotRepository.findByFacilityIdAndStatus(facilityId, ParkingSpot.SpotStatus.AVAILABLE);
    }

    @Cacheable(value = "parkingFacilities", key = "#facilityId")
    public ParkingFacility getCachedFacility(Long facilityId) {
        return facilityRepository.findById(facilityId).orElse(null);
    }

    @Cacheable(value = "parkingFacilities", key = "'nearby:' + #latitude + ':' + #longitude + ':' + #radiusKm")
    public List<ParkingFacility> getCachedNearbyFacilities(Double latitude, Double longitude, Integer radiusKm) {
        return facilityRepository.findNearbyFacilities(
            BigDecimal.valueOf(latitude), 
            BigDecimal.valueOf(longitude), 
            radiusKm * 1000); // Convert km to meters
    }

    // Cache invalidation methods
    @CacheEvict(value = "parkingSpots", allEntries = true)
    public void evictAllSpotsCache() {
        // Evict all parking spots cache
    }

    @CacheEvict(value = "parkingSpots", key = "#facilityId + ':available'")
    public void evictFacilitySpotsCache(Long facilityId) {
        // Evict specific facility spots cache
    }

    @CacheEvict(value = "parkingSpots", key = "#spotId")
    public void evictSpotCache(Long spotId) {
        // Evict specific spot cache
    }

    @CacheEvict(value = "parkingFacilities", key = "#facilityId")
    public void evictFacilityCache(Long facilityId) {
        // Evict specific facility cache
    }

    @CacheEvict(value = "parkingFacilities", allEntries = true)
    public void evictAllFacilitiesCache() {
        // Evict all facilities cache
    }

    // Batch cache warming for high-traffic facilities
    public void warmCache(List<Long> facilityIds) {
        for (Long facilityId : facilityIds) {
            try {
                updateFacilityAvailability(facilityId);
                getCachedAvailableSpots(facilityId);
                getCachedFacility(facilityId);
            } catch (Exception e) {
                System.err.println("Failed to warm cache for facility " + facilityId + ": " + e.getMessage());
            }
        }
    }

    // Performance monitoring
    public CacheStats getCacheStats() {
        try {
            Long availabilityKeys = Long.valueOf(redisTemplate.keys(AVAILABILITY_KEY_PREFIX + "*").size());
            Long spotStatusKeys = Long.valueOf(redisTemplate.keys(SPOT_STATUS_KEY_PREFIX + "*").size());
            Long facilityStatsKeys = Long.valueOf(redisTemplate.keys(FACILITY_STATS_KEY_PREFIX + "*").size());
            
            return new CacheStats(availabilityKeys, spotStatusKeys, facilityStatsKeys);
        } catch (Exception e) {
            return new CacheStats(0L, 0L, 0L);
        }
    }

    // Data classes
    public static class FacilityAvailability {
        public final Long facilityId;
        public final Integer availableSpots;
        public final Integer totalSpots;
        public final LocalDateTime lastUpdated;
        public final Double occupancyRate;

        public FacilityAvailability(Long facilityId, Integer availableSpots, Integer totalSpots, LocalDateTime lastUpdated) {
            this.facilityId = facilityId;
            this.availableSpots = availableSpots != null ? availableSpots : 0;
            this.totalSpots = totalSpots != null ? totalSpots : 0;
            this.lastUpdated = lastUpdated;
            this.occupancyRate = this.totalSpots > 0 ? 
                (double)(this.totalSpots - this.availableSpots) / this.totalSpots * 100 : 0.0;
        }

        public boolean isRecent() {
            return lastUpdated != null && lastUpdated.isAfter(LocalDateTime.now().minusSeconds(30));
        }
    }

    public static class CacheStats {
        public final Long availabilityKeysCount;
        public final Long spotStatusKeysCount;
        public final Long facilityStatsKeysCount;
        public final Long totalKeysCount;

        public CacheStats(Long availabilityKeys, Long spotStatusKeys, Long facilityStatsKeys) {
            this.availabilityKeysCount = availabilityKeys;
            this.spotStatusKeysCount = spotStatusKeys;
            this.facilityStatsKeysCount = facilityStatsKeys;
            this.totalKeysCount = availabilityKeys + spotStatusKeys + facilityStatsKeys;
        }
    }
}