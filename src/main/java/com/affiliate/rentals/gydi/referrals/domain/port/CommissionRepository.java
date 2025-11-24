package com.affiliate.rentals.gydi.referrals.domain.port;

import com.affiliate.rentals.gydi.referrals.domain.model.Commission;
import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de comisiones
 */
public interface CommissionRepository {

    /**
     * Guarda una nueva comisión
     */
    Commission save(Commission commission);

    /**
     * Actualiza el estado de una comisión existente
     * NOTA: Solo se permite actualizar el estado, no los montos (IMMUTABLE ledger)
     */
    Commission updateStatus(Long commissionId, CommissionStatus newStatus);

    /**
     * Busca una comisión por ID
     */
    Optional<Commission> findById(Long id);

    /**
     * Busca todas las comisiones de un afiliado
     */
    List<Commission> findByAffiliateId(Long affiliateId);

    /**
     * Busca comisiones de un afiliado por estado
     */
    List<Commission> findByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);

    /**
     * Busca comisiones de un enlace de referido
     */
    List<Commission> findByReferralLinkId(Long referralLinkId);

    /**
     * Busca comisiones listas para aprobación (PENDING + fuera del período de hold)
     */
    List<Commission> findReadyForApproval(LocalDateTime beforeDate);

    /**
     * Busca comisiones aprobadas listas para pago
     */
    List<Commission> findApprovedForPayout();

    /**
     * Busca comisiones por afiliado en un rango de fechas
     */
    List<Commission> findByAffiliateIdAndDateRange(Long affiliateId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Calcula el total de comisiones ganadas por un afiliado
     */
    BigDecimal calculateTotalEarnings(Long affiliateId);

    /**
     * Calcula comisiones por estado para un afiliado
     */
    BigDecimal calculateEarningsByStatus(Long affiliateId, CommissionStatus status);

    /**
     * Cuenta comisiones por estado para un afiliado
     */
    long countByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);

    /**
     * Calcula comisiones totales por mes para un afiliado
     */
    java.util.Map<String, BigDecimal> calculateMonthlyEarnings(Long affiliateId, int year);
}