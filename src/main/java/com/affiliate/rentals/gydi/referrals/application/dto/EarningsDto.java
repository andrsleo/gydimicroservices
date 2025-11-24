package com.affiliate.rentals.gydi.referrals.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para ganancias del afiliado
 */
public record EarningsDto(
    Long affiliateId,
    String currentPlan,          // FREE, PRO, ELITE
    BigDecimal currentCommissionRate,

    // Totales
    BigDecimal totalEarnings,
    BigDecimal pendingAmount,
    BigDecimal approvedAmount,
    BigDecimal paidAmount,

    // Contadores
    int pendingCount,
    int approvedCount,
    int paidCount,

    // Próximo pago
    BigDecimal nextPayoutAmount,
    LocalDateTime nextPayoutDate,

    // Historial reciente
    List<CommissionDto> recentCommissions
) {
}