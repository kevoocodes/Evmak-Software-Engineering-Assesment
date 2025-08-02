package com.evmak.parking_management.service;

import com.evmak.parking_management.entity.*;
import com.evmak.parking_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ParkingFacilityRepository facilityRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Vehicle testVehicle;
    private ParkingFacility testFacility;
    private ParkingSpot testSpot;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setRole(User.UserRole.USER);
        testUser.setIsActive(true);

        // Create test vehicle
        testVehicle = new Vehicle();
        testVehicle.setId(1L);
        testVehicle.setUser(testUser);
        testVehicle.setLicensePlate("ABC123");
        testVehicle.setVehicleType(Vehicle.VehicleType.CAR);
        testVehicle.setIsActive(true);

        // Create test facility
        testFacility = new ParkingFacility();
        testFacility.setId(1L);
        testFacility.setName("Test Garage");
        testFacility.setFacilityType(ParkingFacility.FacilityType.GARAGE);
        testFacility.setTotalSpots(100);
        testFacility.setAvailableSpots(50);
        testFacility.setBaseHourlyRate(new BigDecimal("10.00"));
        testFacility.setIsActive(true);
        testFacility.setOperatingHoursStart(LocalTime.of(6, 0));
        testFacility.setOperatingHoursEnd(LocalTime.of(22, 0));

        // Create test spot
        testSpot = new ParkingSpot();
        testSpot.setId(1L);
        testSpot.setFacility(testFacility);
        testSpot.setSpotNumber("A001");
        testSpot.setSpotType(ParkingSpot.SpotType.REGULAR);
        testSpot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
    }

    @Test
    void testReserveSpot_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));
        when(spotRepository.findById(1L)).thenReturn(Optional.of(testSpot));
        when(reservationRepository.findByUserIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        when(reservationRepository.findOverlappingReservationsForSpot(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            reservation.setReservationReference("RES-123456789");
            return reservation;
        });
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(testSpot);
        when(facilityRepository.save(any(ParkingFacility.class))).thenReturn(testFacility);

        // Act
        ReservationService.ReservationResult result = reservationService.reserveSpot(1L, 1L, 1L, 1L, 120);

        // Assert
        assertTrue(result.success);
        assertNotNull(result.reservation);
        assertEquals("Spot reserved successfully. You have 15 minutes to confirm your arrival.", result.message);
        assertNull(result.errorCode);

        // Verify interactions
        verify(userRepository).findById(1L);
        verify(vehicleRepository).findById(1L);
        verify(facilityRepository).findById(1L);
        verify(spotRepository).findById(1L);
        verify(reservationRepository).save(any(Reservation.class));
        verify(spotRepository).save(any(ParkingSpot.class));
        verify(facilityRepository).save(any(ParkingFacility.class));
    }

    @Test
    void testReserveSpot_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ReservationService.ReservationResult result = reservationService.reserveSpot(1L, 1L, 1L, 1L, 120);

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("User not found or inactive", result.message);
        assertEquals("USER_INVALID", result.errorCode);
    }

    @Test
    void testReserveSpot_VehicleNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ReservationService.ReservationResult result = reservationService.reserveSpot(1L, 1L, 1L, 1L, 120);

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("Vehicle not found or doesn't belong to user", result.message);
        assertEquals("VEHICLE_INVALID", result.errorCode);
    }

    @Test
    void testReserveSpot_SpotNotAvailable() {
        // Arrange
        testSpot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));
        when(spotRepository.findById(1L)).thenReturn(Optional.of(testSpot));
        when(reservationRepository.findByUserIdAndStatus(anyLong(), any())).thenReturn(Collections.emptyList());

        // Act
        ReservationService.ReservationResult result = reservationService.reserveSpot(1L, 1L, 1L, 1L, 120);

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("Parking spot is not available", result.message);
        assertEquals("SPOT_NOT_AVAILABLE", result.errorCode);
    }

    @Test
    void testReserveSpot_UserHasActiveReservation() {
        // Arrange
        Reservation existingReservation = new Reservation();
        existingReservation.setId(1L);
        existingReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));
        when(reservationRepository.findByUserIdAndStatus(1L, Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Collections.singletonList(existingReservation));

        // Act
        ReservationService.ReservationResult result = reservationService.reserveSpot(1L, 1L, 1L, 1L, 120);

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("User already has an active reservation", result.message);
        assertEquals("ACTIVE_RESERVATION_EXISTS", result.errorCode);
    }

    @Test
    void testConfirmReservation_Success() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationReference("RES-123456789");
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(reservationRepository.findByReservationReference("RES-123456789"))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationService.ReservationResult result = reservationService.confirmReservation("RES-123456789");

        // Assert
        assertTrue(result.success);
        assertNotNull(result.reservation);
        assertEquals("Reservation confirmed successfully", result.message);
        assertEquals(Reservation.ReservationStatus.CONFIRMED, result.reservation.getStatus());
    }

    @Test
    void testConfirmReservation_NotFound() {
        // Arrange
        when(reservationRepository.findByReservationReference("INVALID-REF"))
                .thenReturn(Optional.empty());

        // Act
        ReservationService.ReservationResult result = reservationService.confirmReservation("INVALID-REF");

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("Reservation not found", result.message);
        assertEquals("RESERVATION_NOT_FOUND", result.errorCode);
    }

    @Test
    void testConfirmReservation_Expired() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationReference("RES-123456789");
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(10)); // Expired
        reservation.setSpot(testSpot);

        when(reservationRepository.findByReservationReference("RES-123456789"))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(testSpot);

        // Act
        ReservationService.ReservationResult result = reservationService.confirmReservation("RES-123456789");

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("Reservation has expired", result.message);
        assertEquals("RESERVATION_EXPIRED", result.errorCode);
    }

    @Test
    void testCancelReservation_Success() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationReference("RES-123456789");
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setUser(testUser);
        reservation.setSpot(testSpot);
        reservation.setFacility(testFacility);

        when(reservationRepository.findByReservationReference("RES-123456789"))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(spotRepository.save(any(ParkingSpot.class))).thenReturn(testSpot);
        when(facilityRepository.save(any(ParkingFacility.class))).thenReturn(testFacility);

        // Act
        ReservationService.ReservationResult result = reservationService.cancelReservation("RES-123456789", 1L);

        // Assert
        assertTrue(result.success);
        assertNotNull(result.reservation);
        assertEquals("Reservation cancelled successfully", result.message);
        assertEquals(Reservation.ReservationStatus.CANCELLED, result.reservation.getStatus());
    }

    @Test
    void testCancelReservation_Unauthorized() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationReference("RES-123456789");
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setUser(differentUser); // Different user

        when(reservationRepository.findByReservationReference("RES-123456789"))
                .thenReturn(Optional.of(reservation));

        // Act
        ReservationService.ReservationResult result = reservationService.cancelReservation("RES-123456789", 1L);

        // Assert
        assertFalse(result.success);
        assertNull(result.reservation);
        assertEquals("You can only cancel your own reservations", result.message);
        assertEquals("UNAUTHORIZED", result.errorCode);
    }
}