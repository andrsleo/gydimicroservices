package com.affiliate.rentals.gydi.referrals.application.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO para estadísticas del sistema de referidos
 */
public record ReferralStatsDto(
    Long affiliateId,

    // Estadísticas de enlaces
    int totalLinks,
    int activeLinks,
    int expiredLinks,

    // Estadísticas de clicks
    long totalClicks,
    long uniqueClicks,
    long clicksLast30Days,
    Map<String, Long> clicksByCountry,    // País -> cantidad
    Map<String, Long> clicksByDevice,     // Dispositivo -> cantidad

    // Estadísticas de conversiones
    int totalConversions,
    int conversionsLast30Days,
    double overallConversionRate,

    // Estadísticas financieras
    BigDecimal totalEarnings,
    BigDecimal pendingCommissions,
    BigDecimal approvedCommissions,
    BigDecimal paidCommissions,
    Map<String, BigDecimal> earningsByMonth  // Mes -> monto
) {
}