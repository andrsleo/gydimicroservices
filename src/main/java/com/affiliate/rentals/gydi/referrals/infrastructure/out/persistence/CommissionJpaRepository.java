package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository para Commission
 */
@Repository
public interface CommissionJpaRepository extends JpaRepository<CommissionJpaEntity, Long> {

    /**
     * Busca comisiones por booking ID
     */
    List<CommissionJpaEntity> findByBookingId(Long bookingId);

    /**
     * Busca comisiones por estado
     */
    List<CommissionJpaEntity> findByStatus(CommissionStatus status);

    /**
     * Busca comisiones listas para aprobación (PENDING + creadas hace más de 30 días)
     */
    @Query("SELECT c FROM CommissionJpaEntity c " +
           "WHERE c.status = 'PENDING' " +
           "AND c.createdAt < :beforeDate")
    List<CommissionJpaEntity> findReadyForApproval(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Busca comisiones aprobadas listas para pago
     */
    @Query("SELECT c FROM CommissionJpaEntity c " +
           "WHERE c.status = 'APPROVED'")
    List<CommissionJpaEntity> findApprovedForPayout();

    /**
     * Busca comisiones por affiliate ID (usando JOIN con booking y referral_link)
     */
    @Query(value = "SELECT cb.* FROM referrals.commission_booking cb " +
           "JOIN referrals.booking b ON b.booking_id = cb.booking_id " +
           "JOIN referrals.referral_links rl ON rl.id = b.referral_link_id " +
           "WHERE rl.affiliate_id = :affiliateId " +
           "ORDER BY cb.created_at DESC",
           nativeQuery = true)
    List<CommissionJpaEntity> findByAffiliateId(@Param("affiliateId") Long affiliateId);

    /**
     * Calcula el total de earnings por affiliate ID
     */
    @Query(value = "SELECT COALESCE(SUM(cb.commission_amount), 0) " +
           "FROM referrals.commission_booking cb " +
           "JOIN referrals.booking b ON b.booking_id = cb.booking_id " +
           "JOIN referrals.referral_links rl ON rl.id = b.referral_link_id " +
           "WHERE rl.affiliate_id = :affiliateId",
           nativeQuery = true)
    BigDecimal calculateTotalEarningsByAffiliateId(@Param("affiliateId") Long affiliateId);

    /**
     * Calcula earnings por affiliate ID y status
     */
    @Query(value = "SELECT COALESCE(SUM(cb.commission_amount), 0) " +
           "FROM referrals.commission_booking cb " +
           "JOIN referrals.booking b ON b.booking_id = cb.booking_id " +
           "JOIN referrals.referral_links rl ON rl.id = b.referral_link_id " +
           "WHERE rl.affiliate_id = :affiliateId " +
           "AND cb.status = CAST(:status AS referrals.commission_status)",
           nativeQuery = true)
    BigDecimal calculateEarningsByAffiliateIdAndStatus(
        @Param("affiliateId") Long affiliateId,
        @Param("status") String status
    );

    /**
     * Cuenta comisiones por affiliate ID y status
     */
    @Query(value = "SELECT COUNT(*) " +
           "FROM referrals.commission_booking cb " +
           "JOIN referrals.booking b ON b.booking_id = cb.booking_id " +
           "JOIN referrals.referral_links rl ON rl.id = b.referral_link_id " +
           "WHERE rl.affiliate_id = :affiliateId " +
           "AND cb.status = CAST(:status AS referrals.commission_status)",
           nativeQuery = true)
    long countByAffiliateIdAndStatus(
        @Param("affiliateId") Long affiliateId,
        @Param("status") String status
    );
}