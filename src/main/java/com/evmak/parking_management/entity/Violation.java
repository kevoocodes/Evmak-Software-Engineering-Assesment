package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ParkingSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    @NotNull
    private ParkingSpot spot;
    
    @NotBlank
    @Column(name = "vehicle_license_plate", nullable = false, length = 20)
    private String vehicleLicensePlate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ViolationType violationType;
    
    @NotNull
    @Column(name = "fine_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal fineAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationStatus status = ViolationStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;
    
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ViolationType {
        OVERSTAY, NO_PAYMENT, UNAUTHORIZED_PARKING, DISABLED_SPOT_MISUSE
    }

    public enum ViolationStatus {
        PENDING, PAID, DISPUTED, WAIVED
    }

    // Constructors
    public Violation() {}

    public Violation(ParkingSpot spot, String vehicleLicensePlate, ViolationType violationType, 
                    BigDecimal fineAmount, User reportedBy) {
        this.spot = spot;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.violationType = violationType;
        this.fineAmount = fineAmount;
        this.reportedBy = reportedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ParkingSession getSession() { return session; }
    public void setSession(ParkingSession session) { this.session = session; }

    public ParkingSpot getSpot() { return spot; }
    public void setSpot(ParkingSpot spot) { this.spot = spot; }

    public String getVehicleLicensePlate() { return vehicleLicensePlate; }
    public void setVehicleLicensePlate(String vehicleLicensePlate) { this.vehicleLicensePlate = vehicleLicensePlate; }

    public ViolationType getViolationType() { return violationType; }
    public void setViolationType(ViolationType violationType) { this.violationType = violationType; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public ViolationStatus getStatus() { return status; }
    public void setStatus(ViolationStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public boolean isPending() {
        return status == ViolationStatus.PENDING;
    }

    public boolean isPaid() {
        return status == ViolationStatus.PAID;
    }

    public boolean isDisputed() {
        return status == ViolationStatus.DISPUTED;
    }

    public String getViolationDescription() {
        return switch (violationType) {
            case OVERSTAY -> "Vehicle exceeded maximum parking duration";
            case NO_PAYMENT -> "Vehicle parked without valid payment";
            case UNAUTHORIZED_PARKING -> "Vehicle parked in restricted area";
            case DISABLED_SPOT_MISUSE -> "Non-disabled vehicle using disabled parking spot";
        };
    }

    @Override
    public String toString() {
        return "Violation{" +
                "id=" + id +
                ", vehicleLicensePlate='" + vehicleLicensePlate + '\'' +
                ", violationType=" + violationType +
                ", fineAmount=" + fineAmount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}