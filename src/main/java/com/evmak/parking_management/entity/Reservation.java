package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @NotNull
    private Vehicle vehicle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    @NotNull
    private ParkingFacility facility;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private ParkingSpot spot;
    
    @Column(name = "reservation_reference", unique = true, nullable = false, length = 50)
    private String reservationReference;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;
    
    @NotNull
    @Column(name = "reserved_from", nullable = false)
    private LocalDateTime reservedFrom;
    
    @NotNull
    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;
    
    @NotNull
    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;
    
    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReservationStatus {
        ACTIVE, CONFIRMED, CANCELLED, EXPIRED, COMPLETED
    }

    // Constructors
    public Reservation() {}

    public Reservation(User user, Vehicle vehicle, ParkingFacility facility, 
                      LocalDateTime reservedFrom, LocalDateTime reservedUntil, 
                      BigDecimal hourlyRate) {
        this.user = user;
        this.vehicle = vehicle;
        this.facility = facility;
        this.reservedFrom = reservedFrom;
        this.reservedUntil = reservedUntil;
        this.hourlyRate = hourlyRate;
        this.reservationReference = generateReservationReference();
        this.expiresAt = LocalDateTime.now().plusMinutes(15); // 15 minute window to confirm
        calculateTotalAmount();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public ParkingFacility getFacility() { return facility; }
    public void setFacility(ParkingFacility facility) { this.facility = facility; }

    public ParkingSpot getSpot() { return spot; }
    public void setSpot(ParkingSpot spot) { this.spot = spot; }

    public String getReservationReference() { return reservationReference; }
    public void setReservationReference(String reservationReference) { this.reservationReference = reservationReference; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public LocalDateTime getReservedFrom() { return reservedFrom; }
    public void setReservedFrom(LocalDateTime reservedFrom) { 
        this.reservedFrom = reservedFrom;
        calculateTotalAmount();
    }

    public LocalDateTime getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(LocalDateTime reservedUntil) { 
        this.reservedUntil = reservedUntil;
        calculateTotalAmount();
    }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { 
        this.hourlyRate = hourlyRate;
        calculateTotalAmount();
    }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    private String generateReservationReference() {
        return "RES-" + System.currentTimeMillis();
    }

    private void calculateTotalAmount() {
        if (reservedFrom != null && reservedUntil != null && hourlyRate != null) {
            long minutes = java.time.Duration.between(reservedFrom, reservedUntil).toMinutes();
            long hours = (minutes + 59) / 60; // Round up to next hour
            this.totalAmount = hourlyRate.multiply(BigDecimal.valueOf(hours));
        }
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean isConfirmed() {
        return status == ReservationStatus.CONFIRMED;
    }

    public long getDurationHours() {
        if (reservedFrom != null && reservedUntil != null) {
            return java.time.Duration.between(reservedFrom, reservedUntil).toHours();
        }
        return 0;
    }

    @PrePersist
    private void generateReference() {
        if (reservationReference == null) {
            reservationReference = generateReservationReference();
        }
        calculateTotalAmount();
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", reservationReference='" + reservationReference + '\'' +
                ", status=" + status +
                ", reservedFrom=" + reservedFrom +
                ", reservedUntil=" + reservedUntil +
                ", totalAmount=" + totalAmount +
                ", expiresAt=" + expiresAt +
                '}';
    }
}