package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    
    Optional<ParkingSession> findBySessionReference(String sessionReference);
    
    List<ParkingSession> findByUserIdAndStatus(Long userId, ParkingSession.SessionStatus status);
    
    List<ParkingSession> findByUserId(Long userId);
    
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.spot.id = :spotId AND ps.status = 'ACTIVE'")
    Optional<ParkingSession> findActiveSessionBySpotId(@Param("spotId") Long spotId);
    
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.user.id = :userId AND ps.status = 'ACTIVE'")
    List<ParkingSession> findActiveSessionsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ps) FROM ParkingSession ps WHERE ps.spot.facility.id = :facilityId AND ps.status = 'ACTIVE'")
    Integer countActiveSessionsByFacilityId(@Param("facilityId") Long facilityId);
    
    @Query("""
        SELECT ps FROM ParkingSession ps 
        WHERE ps.startedAt BETWEEN :startDate AND :endDate 
        ORDER BY ps.startedAt DESC
        """)
    List<ParkingSession> findSessionsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("""
        SELECT ps FROM ParkingSession ps 
        WHERE ps.spot.facility.id = :facilityId 
        AND ps.startedAt BETWEEN :startDate AND :endDate 
        ORDER BY ps.startedAt DESC
        """)
    List<ParkingSession> findSessionsByFacilityAndDateRange(@Param("facilityId") Long facilityId,
                                                             @Param("startDate") LocalDateTime startDate, 
                                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("""
        SELECT AVG(ps.actualDurationMinutes) 
        FROM ParkingSession ps 
        WHERE ps.spot.facility.id = :facilityId 
        AND ps.status = 'COMPLETED'
        AND ps.startedAt >= :sinceDate
        """)
    Double getAverageSessionDurationByFacility(@Param("facilityId") Long facilityId, 
                                                @Param("sinceDate") LocalDateTime sinceDate);
    
    @Query("""
        SELECT ps.spot.id, COUNT(ps) as sessionCount
        FROM ParkingSession ps 
        WHERE ps.startedAt BETWEEN :startDate AND :endDate
        GROUP BY ps.spot.id
        ORDER BY sessionCount DESC
        """)
    List<Object[]> findBusiestSpotsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
}