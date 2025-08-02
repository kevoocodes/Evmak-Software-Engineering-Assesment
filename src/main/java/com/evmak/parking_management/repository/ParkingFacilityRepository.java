package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.ParkingFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ParkingFacilityRepository extends JpaRepository<ParkingFacility, Long> {
    
    List<ParkingFacility> findByIsActiveTrue();
    
    List<ParkingFacility> findByFacilityTypeAndIsActiveTrue(ParkingFacility.FacilityType facilityType);
    
    @Query(value = """
        SELECT pf.* FROM parking_facilities pf 
        WHERE pf.is_active = true 
        AND ST_Distance_Sphere(
            POINT(pf.location_lng, pf.location_lat),
            POINT(:lng, :lat)
        ) <= :radiusMeters
        ORDER BY ST_Distance_Sphere(
            POINT(pf.location_lng, pf.location_lat),
            POINT(:lng, :lat)
        ) ASC
        """, nativeQuery = true)
    List<ParkingFacility> findNearbyFacilities(@Param("lat") BigDecimal lat, 
                                               @Param("lng") BigDecimal lng, 
                                               @Param("radiusMeters") Integer radiusMeters);
    
    @Query("SELECT pf FROM ParkingFacility pf WHERE pf.isActive = true AND pf.availableSpots > 0")
    List<ParkingFacility> findFacilitiesWithAvailableSpots();
    
    @Query(value = """
        SELECT pf.* FROM parking_facilities pf 
        WHERE pf.is_active = true 
        AND pf.available_spots > 0
        AND ST_Distance_Sphere(
            POINT(pf.location_lng, pf.location_lat),
            POINT(:lng, :lat)
        ) <= :radiusMeters
        ORDER BY pf.available_spots DESC, ST_Distance_Sphere(
            POINT(pf.location_lng, pf.location_lat),
            POINT(:lng, :lat)
        ) ASC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<ParkingFacility> findAvailableNearbyFacilities(@Param("lat") BigDecimal lat, 
                                                         @Param("lng") BigDecimal lng, 
                                                         @Param("radiusMeters") Integer radiusMeters,
                                                         @Param("maxResults") Integer maxResults);
}