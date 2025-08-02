package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.*;
import com.evmak.parking_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReservationService {

    @Autowired
    private ParkingSpotRepository spotRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private ParkingFacilityRepository facilityRepository;

    // Distributed locking mechanism for high concurrency
    private final ConcurrentHashMap<Long, ReentrantLock> spotLocks = new ConcurrentHashMap<>();

    public static class ReservationResult {
        public final boolean success;
        public final String message;
        public final Reservation reservation;
        public final String errorCode;

        public ReservationResult(boolean success, String message, Reservation reservation, String errorCode) {
            this.success = success;
            this.message = message;
            this.reservation = reservation;
            this.errorCode = errorCode;
        }

        public static ReservationResult success(Reservation reservation, String message) {
            return new ReservationResult(true, message, reservation, null);
        }

        public static ReservationResult failure(String message, String errorCode) {
            return new ReservationResult(false, message, null, errorCode);
        }
    }

    @Transactional
    public ReservationResult reserveSpot(Long userId, Long vehicleId, Long facilityId, 
                                       Long spotId, Integer durationMinutes) {
        
        // Get distributed lock for the specific spot
        ReentrantLock lock = spotLocks.computeIfAbsent(spotId, k -> new ReentrantLock());
        
        if (!lock.tryLock()) {
            return ReservationResult.failure("Spot is currently being reserved by another user", "SPOT_LOCKED");
        }

        try {
            return performReservation(userId, vehicleId, facilityId, spotId, durationMinutes);
        } finally {
            lock.unlock();
        }
    }

    private ReservationResult performReservation(Long userId, Long vehicleId, Long facilityId, 
                                               Long spotId, Integer durationMinutes) {
        
        // Validate user exists and is active
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getIsActive()) {
            return ReservationResult.failure("User not found or inactive", "USER_INVALID");
        }
        User user = userOpt.get();

        // Validate vehicle belongs to user
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty() || !vehicleOpt.get().getUser().getId().equals(userId) || !vehicleOpt.get().getIsActive()) {
            return ReservationResult.failure("Vehicle not found or doesn't belong to user", "VEHICLE_INVALID");
        }
        Vehicle vehicle = vehicleOpt.get();

        // Validate facility exists and is active
        Optional<ParkingFacility> facilityOpt = facilityRepository.findById(facilityId);
        if (facilityOpt.isEmpty() || !facilityOpt.get().getIsActive()) {
            return ReservationResult.failure("Parking facility not found or inactive", "FACILITY_INVALID");
        }
        ParkingFacility facility = facilityOpt.get();

        // Check if user has existing active reservations (limit to 1 active reservation per user)
        List<Reservation> activeReservations = reservationRepository.findByUserIdAndStatus(userId, Reservation.ReservationStatus.ACTIVE);
        if (!activeReservations.isEmpty()) {
            return ReservationResult.failure("User already has an active reservation", "ACTIVE_RESERVATION_EXISTS");
        }

        // Validate spot and check availability
        Optional<ParkingSpot> spotOpt = spotRepository.findById(spotId);
        if (spotOpt.isEmpty()) {
            return ReservationResult.failure("Parking spot not found", "SPOT_NOT_FOUND");
        }
        
        ParkingSpot spot = spotOpt.get();
        
        // Verify spot belongs to the facility
        if (!spot.getFacility().getId().equals(facilityId)) {
            return ReservationResult.failure("Spot does not belong to the specified facility", "SPOT_FACILITY_MISMATCH");
        }

        // Check if spot is available
        if (spot.getStatus() != ParkingSpot.SpotStatus.AVAILABLE) {
            return ReservationResult.failure("Parking spot is not available", "SPOT_NOT_AVAILABLE");
        }

        // Check if spot reservation has expired (cleanup)
        if (spot.getReservationExpiresAt() != null && spot.getReservationExpiresAt().isBefore(LocalDateTime.now())) {
            spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
            spot.setReservedBy(null);
            spot.setReservationExpiresAt(null);
            spotRepository.save(spot);
        }

        // Double-check availability after cleanup
        if (spot.getStatus() != ParkingSpot.SpotStatus.AVAILABLE) {
            return ReservationResult.failure("Parking spot became unavailable", "SPOT_UNAVAILABLE");
        }

        // Calculate reservation times
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservedFrom = now;
        LocalDateTime reservedUntil = now.plusMinutes(durationMinutes);
        LocalDateTime expiresAt = now.plusMinutes(15); // 15-minute hold

        // Check for overlapping reservations
        List<Reservation> overlapping = reservationRepository.findOverlappingReservationsForSpot(
            spotId, reservedFrom, reservedUntil);
        if (!overlapping.isEmpty()) {
            return ReservationResult.failure("Time slot conflicts with existing reservation", "TIME_CONFLICT");
        }

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setVehicle(vehicle);
        reservation.setFacility(facility);
        reservation.setSpot(spot);
        reservation.setReservedFrom(reservedFrom);
        reservation.setReservedUntil(reservedUntil);
        reservation.setHourlyRate(facility.getBaseHourlyRate());
        reservation.setExpiresAt(expiresAt);
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        // Calculate total amount
        long hours = (durationMinutes + 59) / 60; // Round up to next hour
        reservation.setTotalAmount(facility.getBaseHourlyRate().multiply(BigDecimal.valueOf(hours)));

        // Save reservation first
        reservation = reservationRepository.save(reservation);

        // Update spot status
        spot.setStatus(ParkingSpot.SpotStatus.RESERVED);
        spot.setReservedBy(user);
        spot.setReservationExpiresAt(expiresAt);
        spotRepository.save(spot);

        // Update facility available spots count
        facility.setAvailableSpots(facility.getAvailableSpots() - 1);
        facilityRepository.save(facility);

        return ReservationResult.success(reservation, 
            "Spot reserved successfully. You have 15 minutes to confirm your arrival.");
    }

    @Transactional
    public ReservationResult confirmReservation(String reservationReference) {
        Optional<Reservation> reservationOpt = reservationRepository.findByReservationReference(reservationReference);
        
        if (reservationOpt.isEmpty()) {
            return ReservationResult.failure("Reservation not found", "RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();

        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            return ReservationResult.failure("Reservation is not active", "RESERVATION_NOT_ACTIVE");
        }

        if (reservation.isExpired()) {
            // Auto-expire the reservation
            reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            
            // Release the spot
            ParkingSpot spot = reservation.getSpot();
            spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
            spot.setReservedBy(null);
            spot.setReservationExpiresAt(null);
            spotRepository.save(spot);
            
            return ReservationResult.failure("Reservation has expired", "RESERVATION_EXPIRED");
        }

        // Confirm the reservation
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);

        return ReservationResult.success(reservation, "Reservation confirmed successfully");
    }

    @Transactional
    public ReservationResult cancelReservation(String reservationReference, Long userId) {
        Optional<Reservation> reservationOpt = reservationRepository.findByReservationReference(reservationReference);
        
        if (reservationOpt.isEmpty()) {
            return ReservationResult.failure("Reservation not found", "RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();

        // Check if user owns the reservation
        if (!reservation.getUser().getId().equals(userId)) {
            return ReservationResult.failure("You can only cancel your own reservations", "UNAUTHORIZED");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE && 
            reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            return ReservationResult.failure("Reservation cannot be cancelled", "RESERVATION_NOT_CANCELLABLE");
        }

        // Cancel the reservation
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);

        // Release the spot
        ParkingSpot spot = reservation.getSpot();
        spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        spot.setReservedBy(null);
        spot.setReservationExpiresAt(null);
        spotRepository.save(spot);

        // Update facility available spots count
        ParkingFacility facility = reservation.getFacility();
        facility.setAvailableSpots(facility.getAvailableSpots() + 1);
        facilityRepository.save(facility);

        return ReservationResult.success(reservation, "Reservation cancelled successfully");
    }

    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find expired reservations
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(now);
        
        for (Reservation reservation : expiredReservations) {
            // Update reservation status
            reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            
            // Release the spot
            ParkingSpot spot = reservation.getSpot();
            if (spot != null) {
                spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
                spot.setReservedBy(null);
                spot.setReservationExpiresAt(null);
                spotRepository.save(spot);
                
                // Update facility count
                ParkingFacility facility = spot.getFacility();
                facility.setAvailableSpots(facility.getAvailableSpots() + 1);
                facilityRepository.save(facility);
            }
        }
    }

    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}