package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    
    List<ParkingSpot> findByFacilityIdAndStatus(Long facilityId, ParkingSpot.SpotStatus status);
    
    @Query("SELECT COUNT(ps) FROM ParkingSpot ps WHERE ps.facility.id = :facilityId AND ps.status = :status")
    Integer countByFacilityIdAndStatus(@Param("facilityId") Long facilityId, 
                                       @Param("status") ParkingSpot.SpotStatus status);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.facility.id = :facilityId AND ps.status = 'AVAILABLE' AND ps.spotType = :spotType")
    List<ParkingSpot> findAvailableSpotsByTypeInFacility(@Param("facilityId") Long facilityId, 
                                                          @Param("spotType") ParkingSpot.SpotType spotType);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.facility.id = :facilityId AND ps.status = 'AVAILABLE' ORDER BY ps.spotNumber")
    List<ParkingSpot> findAvailableSpotsInFacility(@Param("facilityId") Long facilityId);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.status = 'RESERVED' AND ps.reservationExpiresAt < :currentTime")
    List<ParkingSpot> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    @Modifying
    @Query("UPDATE ParkingSpot ps SET ps.status = 'AVAILABLE', ps.reservedBy = null, ps.reservationExpiresAt = null WHERE ps.status = 'RESERVED' AND ps.reservationExpiresAt < :currentTime")
    Integer releaseExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.facility.id = :facilityId AND ps.status = 'AVAILABLE' AND (:spotType IS NULL OR ps.spotType = :spotType) ORDER BY ps.spotNumber LIMIT 1")
    Optional<ParkingSpot> findFirstAvailableSpot(@Param("facilityId") Long facilityId, 
                                                  @Param("spotType") ParkingSpot.SpotType spotType);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.id = :spotId AND ps.status = 'AVAILABLE'")
    Optional<ParkingSpot> findAvailableSpotById(@Param("spotId") Long spotId);
    
    Optional<ParkingSpot> findByFacilityIdAndSpotNumber(Long facilityId, String spotNumber);
    
    List<ParkingSpot> findByFacilityId(Long facilityId);
    
    @Query("SELECT COUNT(ps) FROM ParkingSpot ps WHERE ps.facility.id = :facilityId AND ps.status = 'AVAILABLE'")
    Integer countAvailableSpotsByFacilityId(@Param("facilityId") Long facilityId);
    
    @Query("SELECT COUNT(ps) FROM ParkingSpot ps WHERE ps.facility.id = :facilityId")
    Integer countSpotsByFacilityId(@Param("facilityId") Long facilityId);
}