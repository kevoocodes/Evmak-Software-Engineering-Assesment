package com.evmak.parking_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "pricing_rules")
public class PricingRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private ParkingFacility facility; // NULL means applies to all facilities
    
    @NotBlank
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;
    
    @Column(nullable = false)
    private Integer priority = 0; // Higher priority rules override lower ones
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "pricing_rule_days", joinColumns = @JoinColumn(name = "pricing_rule_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek;
    
    @Column(name = "demand_threshold_percentage", precision = 5, scale = 2)
    private BigDecimal demandThresholdPercentage;
    
    @NotNull
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal multiplier = BigDecimal.ONE;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RuleType {
        TIME_BASED, DEMAND_BASED, EVENT_BASED, WEATHER_BASED
    }

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    // Constructors
    public PricingRule() {}

    public PricingRule(String ruleName, RuleType ruleType, BigDecimal multiplier) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.multiplier = multiplier;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ParkingFacility getFacility() { return facility; }
    public void setFacility(ParkingFacility facility) { this.facility = facility; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public BigDecimal getDemandThresholdPercentage() { return demandThresholdPercentage; }
    public void setDemandThresholdPercentage(BigDecimal demandThresholdPercentage) { this.demandThresholdPercentage = demandThresholdPercentage; }

    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public boolean isApplicableNow() {
        if (!isActive) return false;
        
        LocalTime now = LocalTime.now();
        java.time.DayOfWeek currentDay = java.time.LocalDate.now().getDayOfWeek();
        
        // Check time range
        if (startTime != null && endTime != null) {
            if (now.isBefore(startTime) || now.isAfter(endTime)) {
                return false;
            }
        }
        
        // Check day of week
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            DayOfWeek currentDayEnum = DayOfWeek.valueOf(currentDay.name());
            if (!daysOfWeek.contains(currentDayEnum)) {
                return false;
            }
        }
        
        return true;
    }

    public boolean isApplicableForDemand(BigDecimal currentOccupancyPercentage) {
        if (ruleType != RuleType.DEMAND_BASED) return true;
        if (demandThresholdPercentage == null) return true;
        
        return currentOccupancyPercentage.compareTo(demandThresholdPercentage) >= 0;
    }

    public boolean appliesToFacility(Long facilityId) {
        return facility == null || (facility.getId().equals(facilityId));
    }

    @Override
    public String toString() {
        return "PricingRule{" +
                "id=" + id +
                ", ruleName='" + ruleName + '\'' +
                ", ruleType=" + ruleType +
                ", priority=" + priority +
                ", multiplier=" + multiplier +
                ", isActive=" + isActive +
                '}';
    }
}