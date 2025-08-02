package com.evmak.parking_management.controller;

import com.evmak.parking_management.entity.ParkingSession;
import com.evmak.parking_management.repository.ParkingSessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Parking Sessions", description = "Manage parking sessions and vehicle check-ins/check-outs")
public class ParkingSessionController {

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @GetMapping
    @Operation(summary = "Get all sessions", description = "Retrieve all parking sessions")
    public ResponseEntity<List<ParkingSession>> getAllSessions() {
        List<ParkingSession> sessions = sessionRepository.findAll();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID", description = "Retrieve a specific parking session")
    public ResponseEntity<ParkingSession> getSessionById(@PathVariable Long id) {
        Optional<ParkingSession> session = sessionRepository.findById(id);
        return session.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get session by reference", description = "Find session by unique reference")
    public ResponseEntity<ParkingSession> getSessionByReference(@PathVariable String reference) {
        Optional<ParkingSession> session = sessionRepository.findBySessionReference(reference);
        return session.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get sessions by user", description = "Get all sessions for a specific user")
    public ResponseEntity<List<ParkingSession>> getSessionsByUser(@PathVariable Long userId) {
        List<ParkingSession> sessions = sessionRepository.findByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active sessions by user", description = "Get active sessions for a specific user")
    public ResponseEntity<List<ParkingSession>> getActiveSessionsByUser(@PathVariable Long userId) {
        List<ParkingSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/spot/{spotId}/active")
    @Operation(summary = "Get active session for spot", description = "Get the active session for a specific spot")
    public ResponseEntity<ParkingSession> getActiveSessionBySpot(@PathVariable Long spotId) {
        Optional<ParkingSession> session = sessionRepository.findActiveSessionBySpotId(spotId);
        return session.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/facility/{facilityId}/count")
    @Operation(summary = "Count active sessions in facility", description = "Count active sessions in a facility")
    public ResponseEntity<Integer> countActiveSessionsByFacility(@PathVariable Long facilityId) {
        Integer count = sessionRepository.countActiveSessionsByFacilityId(facilityId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/search/date-range")
    @Operation(summary = "Search sessions by date range", description = "Find sessions within a date range")
    public ResponseEntity<List<ParkingSession>> getSessionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ParkingSession> sessions = sessionRepository.findSessionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/facility/{facilityId}/date-range")
    @Operation(summary = "Get facility sessions by date range", description = "Find sessions in a facility within a date range")
    public ResponseEntity<List<ParkingSession>> getSessionsByFacilityAndDateRange(
            @PathVariable Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ParkingSession> sessions = sessionRepository.findSessionsByFacilityAndDateRange(facilityId, startDate, endDate);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/facility/{facilityId}/average-duration")
    @Operation(summary = "Get average session duration", description = "Calculate average session duration for a facility")
    public ResponseEntity<Double> getAverageSessionDuration(
            @PathVariable Long facilityId,
            @RequestParam(defaultValue = "30") Integer daysSince) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(daysSince);
        Double avgDuration = sessionRepository.getAverageSessionDurationByFacility(facilityId, sinceDate);
        return ResponseEntity.ok(avgDuration != null ? avgDuration : 0.0);
    }

    @GetMapping("/analytics/busiest-spots")
    @Operation(summary = "Get busiest parking spots", description = "Find the busiest parking spots by session count")
    public ResponseEntity<List<Object[]>> getBusiestSpots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Object[]> busiestSpots = sessionRepository.findBusiestSpotsByDateRange(startDate, endDate);
        return ResponseEntity.ok(busiestSpots);
    }

    @PostMapping("/start")
    @Operation(summary = "Start parking session", description = "Start a new parking session")
    public ResponseEntity<ParkingSession> startSession(@RequestBody ParkingSession session) {
        session.setStatus(ParkingSession.SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        ParkingSession savedSession = sessionRepository.save(session);
        return ResponseEntity.ok(savedSession);
    }

    @PutMapping("/{id}/end")
    @Operation(summary = "End parking session", description = "End an active parking session")
    public ResponseEntity<ParkingSession> endSession(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(session -> {
                if (session.getStatus() == ParkingSession.SessionStatus.ACTIVE) {
                    session.setStatus(ParkingSession.SessionStatus.COMPLETED);
                    session.setEndedAt(LocalDateTime.now());
                    
                    // Calculate duration and total amount
                    long minutes = session.getCurrentDurationMinutes();
                    session.setActualDurationMinutes((int) minutes);
                    session.setTotalAmount(session.calculateCurrentAmount());
                    
                    ParkingSession updatedSession = sessionRepository.save(session);
                    return ResponseEntity.ok(updatedSession);
                } else {
                    return ResponseEntity.badRequest().<ParkingSession>build();
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel parking session", description = "Cancel an active parking session")
    public ResponseEntity<ParkingSession> cancelSession(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(session -> {
                if (session.getStatus() == ParkingSession.SessionStatus.ACTIVE) {
                    session.setStatus(ParkingSession.SessionStatus.CANCELLED);
                    session.setEndedAt(LocalDateTime.now());
                    ParkingSession updatedSession = sessionRepository.save(session);
                    return ResponseEntity.ok(updatedSession);
                } else {
                    return ResponseEntity.badRequest().<ParkingSession>build();
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update parking session", description = "Update an existing parking session")
    public ResponseEntity<ParkingSession> updateSession(@PathVariable Long id, @RequestBody ParkingSession sessionDetails) {
        return sessionRepository.findById(id)
            .map(session -> {
                session.setPlannedDurationHours(sessionDetails.getPlannedDurationHours());
                ParkingSession updatedSession = sessionRepository.save(session);
                return ResponseEntity.ok(updatedSession);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete parking session", description = "Mark a parking session as expired")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(session -> {
                session.setStatus(ParkingSession.SessionStatus.EXPIRED);
                sessionRepository.save(session);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}