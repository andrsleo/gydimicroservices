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
     * Busca comisiones por booking ID
     */
    List<Commission> findByBookingId(Long bookingId);

    /**
     * Busca comisiones por estado
     */
    List<Commission> findByStatus(CommissionStatus status);

    /**
     * Busca comisiones listas para aprobación (PENDING + fuera del período de hold)
     */
    List<Commission> findReadyForApproval(LocalDateTime beforeDate);

    /**
     * Busca comisiones aprobadas listas para pago
     */
    List<Commission> findApprovedForPayout();

    /**
     * Busca comisiones por affiliate ID (usando JOIN con booking -> referral_link)
     */
    List<Commission> findByAffiliateId(Long affiliateId);

    /**
     * Calcula el total de earnings por affiliate ID
     */
    BigDecimal calculateTotalEarningsByAffiliateId(Long affiliateId);

    /**
     * Calcula earnings por affiliate ID y status
     */
    BigDecimal calculateEarningsByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);

    /**
     * Cuenta comisiones por affiliate ID y status
     */
    long countByAffiliateIdAndStatus(Long affiliateId, CommissionStatus status);
}