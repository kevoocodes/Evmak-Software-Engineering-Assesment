package com.evmak.parking_management.repository;

import com.evmak.parking_management.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
    
    List<Payment> findBySessionId(Long sessionId);
    
    List<Payment> findBySessionIdAndStatus(Long sessionId, Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findStalePayments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("""
        SELECT SUM(p.amount) 
        FROM Payment p 
        WHERE p.status = 'COMPLETED' 
        AND p.completedAt BETWEEN :startDate AND :endDate
        """)
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("""
        SELECT p.paymentMethod, SUM(p.amount), COUNT(p)
        FROM Payment p 
        WHERE p.status = 'COMPLETED' 
        AND p.completedAt BETWEEN :startDate AND :endDate
        GROUP BY p.paymentMethod
        """)
    List<Object[]> getPaymentMethodStatistics(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("""
        SELECT SUM(p.amount) 
        FROM Payment p 
        JOIN p.session ps 
        WHERE p.status = 'COMPLETED' 
        AND ps.spot.facility.id = :facilityId
        AND p.completedAt BETWEEN :startDate AND :endDate
        """)
    BigDecimal getRevenueByFacilityAndDateRange(@Param("facilityId") Long facilityId,
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
}