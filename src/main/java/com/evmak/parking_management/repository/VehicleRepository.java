package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    
    List<Vehicle> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("SELECT v FROM Vehicle v WHERE v.user.id = :userId AND v.isActive = true ORDER BY v.createdAt DESC")
    List<Vehicle> findActiveVehiclesByUserIdOrderByCreatedAt(@Param("userId") Long userId);
    
    boolean existsByLicensePlate(String licensePlate);
    
    @Query("SELECT v FROM Vehicle v WHERE v.licensePlate = :licensePlate AND v.isActive = true")
    Optional<Vehicle> findActiveVehicleByLicensePlate(@Param("licensePlate") String licensePlate);
}