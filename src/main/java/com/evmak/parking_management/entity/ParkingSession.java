package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_sessions")
public class ParkingSession {
    
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
    @JoinColumn(name = "spot_id", nullable = false)
    @NotNull
    private ParkingSpot spot;
    
    @Column(name = "session_reference", unique = true, nullable = false, length = 50)
    private String sessionReference;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Column(name = "planned_duration_hours")
    private Integer plannedDurationHours;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @NotNull
    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Violation> violations = new ArrayList<>();

    public enum SessionStatus {
        ACTIVE, COMPLETED, CANCELLED, EXPIRED
    }

    // Constructors
    public ParkingSession() {}

    public ParkingSession(User user, Vehicle vehicle, ParkingSpot spot, BigDecimal hourlyRate) {
        this.user = user;
        this.vehicle = vehicle;
        this.spot = spot;
        this.hourlyRate = hourlyRate;
        this.sessionReference = generateSessionReference();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public ParkingSpot getSpot() { return spot; }
    public void setSpot(ParkingSpot spot) { this.spot = spot; }

    public String getSessionReference() { return sessionReference; }
    public void setSessionReference(String sessionReference) { this.sessionReference = sessionReference; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public Integer getPlannedDurationHours() { return plannedDurationHours; }
    public void setPlannedDurationHours(Integer plannedDurationHours) { this.plannedDurationHours = plannedDurationHours; }

    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(Integer actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public List<Violation> getViolations() { return violations; }
    public void setViolations(List<Violation> violations) { this.violations = violations; }

    // Utility methods
    private String generateSessionReference() {
        return "PARK-" + System.currentTimeMillis();
    }

    public long getCurrentDurationMinutes() {
        LocalDateTime endTime = endedAt != null ? endedAt : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(startedAt, endTime);
    }

    public BigDecimal calculateCurrentAmount() {
        long minutes = getCurrentDurationMinutes();
        // Round up to next hour for billing
        long hours = (minutes + 59) / 60;
        return hourlyRate.multiply(BigDecimal.valueOf(hours));
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    @PrePersist
    private void generateReference() {
        if (sessionReference == null) {
            sessionReference = generateSessionReference();
        }
    }

    @Override
    public String toString() {
        return "ParkingSession{" +
                "id=" + id +
                ", sessionReference='" + sessionReference + '\'' +
                ", status=" + status +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", hourlyRate=" + hourlyRate +
                ", totalAmount=" + totalAmount +
                '}';
    }
}