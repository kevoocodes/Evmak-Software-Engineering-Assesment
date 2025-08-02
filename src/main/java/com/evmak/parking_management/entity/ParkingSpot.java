package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_spots")
public class ParkingSpot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    @NotNull
    private ParkingFacility facility;
    
    @NotBlank
    @Column(name = "spot_number", nullable = false, length = 20)
    private String spotNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false)
    private SpotType spotType = SpotType.REGULAR;
    
    @Column(name = "floor_level")
    private Integer floorLevel = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status = SpotStatus.AVAILABLE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by")
    private User reservedBy;
    
    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;
    
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParkingSession> parkingSessions = new ArrayList<>();
    
    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Violation> violations = new ArrayList<>();

    public enum SpotType {
        REGULAR, DISABLED, ELECTRIC, COMPACT
    }

    public enum SpotStatus {
        AVAILABLE, OCCUPIED, RESERVED, OUT_OF_ORDER
    }

    // Constructors
    public ParkingSpot() {}

    public ParkingSpot(ParkingFacility facility, String spotNumber, SpotType spotType) {
        this.facility = facility;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ParkingFacility getFacility() { return facility; }
    public void setFacility(ParkingFacility facility) { this.facility = facility; }

    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }

    public SpotType getSpotType() { return spotType; }
    public void setSpotType(SpotType spotType) { this.spotType = spotType; }

    public Integer getFloorLevel() { return floorLevel; }
    public void setFloorLevel(Integer floorLevel) { this.floorLevel = floorLevel; }

    public SpotStatus getStatus() { return status; }
    public void setStatus(SpotStatus status) { this.status = status; }

    public User getReservedBy() { return reservedBy; }
    public void setReservedBy(User reservedBy) { this.reservedBy = reservedBy; }

    public LocalDateTime getReservationExpiresAt() { return reservationExpiresAt; }
    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) { this.reservationExpiresAt = reservationExpiresAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ParkingSession> getParkingSessions() { return parkingSessions; }
    public void setParkingSessions(List<ParkingSession> parkingSessions) { this.parkingSessions = parkingSessions; }

    public List<Violation> getViolations() { return violations; }
    public void setViolations(List<Violation> violations) { this.violations = violations; }

    // Utility methods
    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public boolean isReserved() {
        return status == SpotStatus.RESERVED;
    }

    public boolean isReservationExpired() {
        return reservationExpiresAt != null && reservationExpiresAt.isBefore(LocalDateTime.now());
    }

    public String getFullSpotIdentifier() {
        return facility.getName() + " - " + spotNumber;
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id=" + id +
                ", spotNumber='" + spotNumber + '\'' +
                ", spotType=" + spotType +
                ", floorLevel=" + floorLevel +
                ", status=" + status +
                ", reservationExpiresAt=" + reservationExpiresAt +
                '}';
    }
}