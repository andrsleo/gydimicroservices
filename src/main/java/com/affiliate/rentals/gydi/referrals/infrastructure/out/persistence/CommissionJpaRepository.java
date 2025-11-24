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
     * Busca todas las comisiones de un afiliado
     */
    List<CommissionJpaEntity> findByAffiliateId(Long affiliateId);

    /**
     * Busca comisiones de un afiliado por estado
     */
    List<CommissionJpaEntity> findByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);

    /**
     * Busca comisiones de un enlace de referido
     */
    List<CommissionJpaEntity> findByReferralLinkId(Long referralLinkId);

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
     * Busca comisiones por afiliado en un rango de fechas
     */
    @Query("SELECT c FROM CommissionJpaEntity c " +
           "WHERE c.affiliateId = :affiliateId " +
           "AND c.createdAt BETWEEN :from AND :to")
    List<CommissionJpaEntity> findByAffiliateIdAndDateRange(
        @Param("affiliateId") Long affiliateId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Calcula el total de comisiones ganadas por un afiliado
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionJpaEntity c " +
           "WHERE c.affiliateId = :affiliateId")
    BigDecimal calculateTotalEarnings(@Param("affiliateId") Long affiliateId);

    /**
     * Calcula comisiones por estado para un afiliado
     */
    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionJpaEntity c " +
           "WHERE c.affiliateId = :affiliateId " +
           "AND c.status = :status")
    BigDecimal calculateEarningsByStatus(
        @Param("affiliateId") Long affiliateId,
        @Param("status") CommissionStatus status
    );

    /**
     * Cuenta comisiones por estado para un afiliado
     */
    long countByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);
}