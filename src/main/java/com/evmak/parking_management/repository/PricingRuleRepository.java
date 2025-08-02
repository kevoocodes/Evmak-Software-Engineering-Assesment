package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    
    @Query("""
        SELECT pr FROM PricingRule pr 
        WHERE pr.isActive = true 
        AND (pr.facility IS NULL OR pr.facility.id = :facilityId)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findApplicableRulesForFacility(@Param("facilityId") Long facilityId);
    
    List<PricingRule> findByIsActiveTrueOrderByPriorityDesc();
    
    List<PricingRule> findByRuleTypeAndIsActiveTrue(PricingRule.RuleType ruleType);
    
    @Query("""
        SELECT pr FROM PricingRule pr 
        WHERE pr.isActive = true 
        AND pr.ruleType = :ruleType
        AND (pr.facility IS NULL OR pr.facility.id = :facilityId)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findByRuleTypeAndFacilityOrderByPriority(@Param("ruleType") PricingRule.RuleType ruleType,
                                                                @Param("facilityId") Long facilityId);
}