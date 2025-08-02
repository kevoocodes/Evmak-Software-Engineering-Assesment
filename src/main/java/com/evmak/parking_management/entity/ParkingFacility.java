package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_facilities")
public class ParkingFacility {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false)
    private FacilityType facilityType;
    
    @NotBlank
    @Column(nullable = false)
    private String address;
    
    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(name = "location_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal locationLat;
    
    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(name = "location_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal locationLng;
    
    @Column(name = "total_spots", nullable = false)
    private Integer totalSpots = 0;
    
    @Column(name = "available_spots", nullable = false)
    private Integer availableSpots = 0;
    
    @NotNull
    @Column(name = "base_hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseHourlyRate;
    
    @Column(name = "max_hours")
    private Integer maxHours = 24;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "operating_hours_start")
    private LocalTime operatingHoursStart = LocalTime.of(0, 0);
    
    @Column(name = "operating_hours_end")
    private LocalTime operatingHoursEnd = LocalTime.of(23, 59);
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PricingRule> pricingRules = new ArrayList<>();
    
    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    public enum FacilityType {
        GARAGE, STREET_ZONE
    }

    // Constructors
    public ParkingFacility() {}

    public ParkingFacility(String name, FacilityType facilityType, String address, 
                          BigDecimal locationLat, BigDecimal locationLng, BigDecimal baseHourlyRate) {
        this.name = name;
        this.facilityType = facilityType;
        this.address = address;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.baseHourlyRate = baseHourlyRate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public FacilityType getFacilityType() { return facilityType; }
    public void setFacilityType(FacilityType facilityType) { this.facilityType = facilityType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLocationLat() { return locationLat; }
    public void setLocationLat(BigDecimal locationLat) { this.locationLat = locationLat; }

    public BigDecimal getLocationLng() { return locationLng; }
    public void setLocationLng(BigDecimal locationLng) { this.locationLng = locationLng; }

    public Integer getTotalSpots() { return totalSpots; }
    public void setTotalSpots(Integer totalSpots) { this.totalSpots = totalSpots; }

    public Integer getAvailableSpots() { return availableSpots; }
    public void setAvailableSpots(Integer availableSpots) { this.availableSpots = availableSpots; }

    public BigDecimal getBaseHourlyRate() { return baseHourlyRate; }
    public void setBaseHourlyRate(BigDecimal baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }

    public Integer getMaxHours() { return maxHours; }
    public void setMaxHours(Integer maxHours) { this.maxHours = maxHours; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalTime getOperatingHoursStart() { return operatingHoursStart; }
    public void setOperatingHoursStart(LocalTime operatingHoursStart) { this.operatingHoursStart = operatingHoursStart; }

    public LocalTime getOperatingHoursEnd() { return operatingHoursEnd; }
    public void setOperatingHoursEnd(LocalTime operatingHoursEnd) { this.operatingHoursEnd = operatingHoursEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ParkingSpot> getParkingSpots() { return parkingSpots; }
    public void setParkingSpots(List<ParkingSpot> parkingSpots) { this.parkingSpots = parkingSpots; }

    public List<PricingRule> getPricingRules() { return pricingRules; }
    public void setPricingRules(List<PricingRule> pricingRules) { this.pricingRules = pricingRules; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }

    // Utility methods
    public Double getOccupancyRate() {
        if (totalSpots == 0) return 0.0;
        return (double)(totalSpots - availableSpots) / totalSpots * 100.0;
    }

    public boolean isOperating(LocalTime time) {
        return !time.isBefore(operatingHoursStart) && !time.isAfter(operatingHoursEnd);
    }

    @Override
    public String toString() {
        return "ParkingFacility{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", facilityType=" + facilityType +
                ", address='" + address + '\'' +
                ", totalSpots=" + totalSpots +
                ", availableSpots=" + availableSpots +
                ", baseHourlyRate=" + baseHourlyRate +
                ", isActive=" + isActive +
                '}';
    }
}