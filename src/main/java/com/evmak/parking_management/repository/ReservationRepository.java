package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    Optional<Reservation> findByReservationReference(String reservationReference);
    
    List<Reservation> findByUserIdAndStatus(Long userId, Reservation.ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :currentTime")
    List<Reservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'EXPIRED' WHERE r.status = 'ACTIVE' AND r.expiresAt < :currentTime")
    Integer expireOldReservations(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.facility.id = :facilityId 
        AND r.status IN ('ACTIVE', 'CONFIRMED')
        AND r.reservedFrom <= :endTime 
        AND r.reservedUntil >= :startTime
        """)
    List<Reservation> findOverlappingReservations(@Param("facilityId") Long facilityId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
    
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.spot.id = :spotId 
        AND r.status IN ('ACTIVE', 'CONFIRMED')
        AND r.reservedFrom <= :endTime 
        AND r.reservedUntil >= :startTime
        """)
    List<Reservation> findOverlappingReservationsForSpot(@Param("spotId") Long spotId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Reservation> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}