package com.evmak.parking_management.controller;

import com.evmak.parking_management.entity.ParkingSpot;
import com.evmak.parking_management.repository.ParkingSpotRepository;
import com.evmak.parking_management.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/spots")
@Tag(name = "Parking Spots", description = "Manage individual parking spots")
public class ParkingSpotController {

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private CacheService cacheService;

    @GetMapping("/facility/{facilityId}")
    @Operation(summary = "Get spots by facility", description = "Get all parking spots in a facility")
    public ResponseEntity<List<ParkingSpot>> getSpotsByFacility(@PathVariable Long facilityId) {
        List<ParkingSpot> spots = spotRepository.findByFacilityId(facilityId);
        return ResponseEntity.ok(spots);
    }

    @GetMapping("/facility/{facilityId}/available")
    @Operation(summary = "Get available spots in facility", description = "Get all available spots in a facility (cached for performance)")
    public ResponseEntity<List<ParkingSpot>> getAvailableSpots(@PathVariable Long facilityId) {
        List<ParkingSpot> spots = cacheService.getCachedAvailableSpots(facilityId);
        return ResponseEntity.ok(spots);
    }

    @GetMapping("/facility/{facilityId}/available/type/{spotType}")
    @Operation(summary = "Get available spots by type", description = "Get available spots of specific type in a facility")
    public ResponseEntity<List<ParkingSpot>> getAvailableSpotsByType(
            @PathVariable Long facilityId,
            @PathVariable ParkingSpot.SpotType spotType) {
        List<ParkingSpot> spots = spotRepository.findAvailableSpotsByTypeInFacility(facilityId, spotType);
        return ResponseEntity.ok(spots);
    }

    @GetMapping("/facility/{facilityId}/count/{status}")
    @Operation(summary = "Count spots by status", description = "Count spots by status in a facility")
    public ResponseEntity<Integer> countSpotsByStatus(
            @PathVariable Long facilityId,
            @PathVariable ParkingSpot.SpotStatus status) {
        Integer count = spotRepository.countByFacilityIdAndStatus(facilityId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get spot by ID", description = "Get a specific parking spot")
    public ResponseEntity<ParkingSpot> getSpotById(@PathVariable Long id) {
        Optional<ParkingSpot> spot = spotRepository.findById(id);
        return spot.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/facility/{facilityId}/reserve/{spotId}")
    @Operation(summary = "Reserve a parking spot", description = "Reserve a specific parking spot for a user")
    public ResponseEntity<String> reserveSpot(
            @PathVariable Long facilityId,
            @PathVariable Long spotId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "15") Integer reservationMinutes) {
        
        Optional<ParkingSpot> spotOpt = spotRepository.findAvailableSpotById(spotId);
        
        if (spotOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Spot not available");
        }
        
        ParkingSpot spot = spotOpt.get();
        
        // Simple reservation logic (will be improved with concurrency handling later)
        spot.setStatus(ParkingSpot.SpotStatus.RESERVED);
        spot.setReservationExpiresAt(LocalDateTime.now().plusMinutes(reservationMinutes));
        // Note: We need to handle the User entity properly, for now using null
        // spot.setReservedBy(userRepository.findById(userId).orElse(null));
        
        spotRepository.save(spot);
        
        return ResponseEntity.ok("Spot reserved successfully for " + reservationMinutes + " minutes");
    }

    @PostMapping("/facility/{facilityId}/find-available")
    @Operation(summary = "Find first available spot", description = "Find the first available spot in a facility")
    public ResponseEntity<ParkingSpot> findFirstAvailableSpot(
            @PathVariable Long facilityId,
            @RequestParam(required = false) ParkingSpot.SpotType spotType) {
        
        Optional<ParkingSpot> spot = spotRepository.findFirstAvailableSpot(facilityId, spotType);
        return spot.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update spot status", description = "Update the status of a parking spot")
    public ResponseEntity<ParkingSpot> updateSpotStatus(
            @PathVariable Long id,
            @RequestParam ParkingSpot.SpotStatus status) {
        
        return spotRepository.findById(id)
            .map(spot -> {
                spot.setStatus(status);
                if (status == ParkingSpot.SpotStatus.AVAILABLE) {
                    spot.setReservedBy(null);
                    spot.setReservationExpiresAt(null);
                }
                ParkingSpot updatedSpot = spotRepository.save(spot);
                return ResponseEntity.ok(updatedSpot);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cleanup-expired")
    @Operation(summary = "Clean up expired reservations", description = "Release spots with expired reservations")
    public ResponseEntity<String> cleanupExpiredReservations() {
        Integer releasedCount = spotRepository.releaseExpiredReservations(LocalDateTime.now());
        return ResponseEntity.ok("Released " + releasedCount + " expired reservations");
    }

    @PostMapping
    @Operation(summary = "Create new parking spot", description = "Add a new parking spot to a facility")
    public ResponseEntity<ParkingSpot> createSpot(@RequestBody ParkingSpot spot) {
        ParkingSpot savedSpot = spotRepository.save(spot);
        return ResponseEntity.ok(savedSpot);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete parking spot", description = "Mark a parking spot as out of order")
    public ResponseEntity<?> deleteSpot(@PathVariable Long id) {
        return spotRepository.findById(id)
            .map(spot -> {
                spot.setStatus(ParkingSpot.SpotStatus.OUT_OF_ORDER);
                spotRepository.save(spot);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}