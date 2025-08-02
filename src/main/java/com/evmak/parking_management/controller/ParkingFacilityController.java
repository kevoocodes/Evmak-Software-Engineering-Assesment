package com.evmak.parking_management.controller;

import com.evmak.parking_management.entity.ParkingFacility;
import com.evmak.parking_management.repository.ParkingFacilityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/facilities")
@Tag(name = "Parking Facilities", description = "Manage parking facilities and find available spots")
public class ParkingFacilityController {

    @Autowired
    private ParkingFacilityRepository facilityRepository;

    @GetMapping
    @Operation(summary = "Get all parking facilities", description = "Retrieve all active parking facilities")
    public ResponseEntity<List<ParkingFacility>> getAllFacilities() {
        List<ParkingFacility> facilities = facilityRepository.findByIsActiveTrue();
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get facility by ID", description = "Retrieve a specific parking facility")
    public ResponseEntity<ParkingFacility> getFacilityById(@PathVariable Long id) {
        Optional<ParkingFacility> facility = facilityRepository.findById(id);
        return facility.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/nearby")
    @Operation(summary = "Find nearby facilities", description = "Search for parking facilities within a radius")
    public ResponseEntity<List<ParkingFacility>> findNearbyFacilities(
            @RequestParam("lat") BigDecimal latitude,
            @RequestParam("lng") BigDecimal longitude,
            @RequestParam(value = "radius", defaultValue = "1000") Integer radiusMeters,
            @RequestParam(value = "maxResults", defaultValue = "10") Integer maxResults) {
        
        List<ParkingFacility> facilities = facilityRepository.findAvailableNearbyFacilities(
            latitude, longitude, radiusMeters, maxResults);
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/available")
    @Operation(summary = "Get facilities with available spots", description = "Find all facilities that have available parking spots")
    public ResponseEntity<List<ParkingFacility>> getFacilitiesWithAvailableSpots() {
        List<ParkingFacility> facilities = facilityRepository.findFacilitiesWithAvailableSpots();
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/type/{facilityType}")
    @Operation(summary = "Get facilities by type", description = "Filter facilities by type (GARAGE or STREET_ZONE)")
    public ResponseEntity<List<ParkingFacility>> getFacilitiesByType(
            @PathVariable ParkingFacility.FacilityType facilityType) {
        List<ParkingFacility> facilities = facilityRepository.findByFacilityTypeAndIsActiveTrue(facilityType);
        return ResponseEntity.ok(facilities);
    }

    @PostMapping
    @Operation(summary = "Create new facility", description = "Add a new parking facility")
    public ResponseEntity<ParkingFacility> createFacility(@RequestBody ParkingFacility facility) {
        ParkingFacility savedFacility = facilityRepository.save(facility);
        return ResponseEntity.ok(savedFacility);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update facility", description = "Update an existing parking facility")
    public ResponseEntity<ParkingFacility> updateFacility(
            @PathVariable Long id, 
            @RequestBody ParkingFacility facilityDetails) {
        
        return facilityRepository.findById(id)
            .map(facility -> {
                facility.setName(facilityDetails.getName());
                facility.setAddress(facilityDetails.getAddress());
                facility.setLocationLat(facilityDetails.getLocationLat());
                facility.setLocationLng(facilityDetails.getLocationLng());
                facility.setBaseHourlyRate(facilityDetails.getBaseHourlyRate());
                facility.setMaxHours(facilityDetails.getMaxHours());
                facility.setOperatingHoursStart(facilityDetails.getOperatingHoursStart());
                facility.setOperatingHoursEnd(facilityDetails.getOperatingHoursEnd());
                facility.setIsActive(facilityDetails.getIsActive());
                
                ParkingFacility updatedFacility = facilityRepository.save(facility);
                return ResponseEntity.ok(updatedFacility);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete facility", description = "Deactivate a parking facility")
    public ResponseEntity<?> deleteFacility(@PathVariable Long id) {
        return facilityRepository.findById(id)
            .map(facility -> {
                facility.setIsActive(false);
                facilityRepository.save(facility);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}