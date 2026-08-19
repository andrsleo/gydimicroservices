package com.affiliate.rentals.gydi.commissions.application.dto;

import java.math.BigDecimal;

/**
 * DTO for Commission statistics.
 * <p>
 * Used for dashboard stats for affiliates and hosts.
 * </p>
 *
 * SEMANTIC CLARITY:
 * - totalEarned: For AFFILIATES (platform PAYS them)
 * - totalPaid: For HOSTS (platform CHARGES them)
 */
public record CommissionStatsDto(
    BigDecimal totalEarned,
    BigDecimal totalPaid,
    BigDecimal pending,
    String currency
) {
    public CommissionStatsDto {
        // Set default currency if null
        currency = currency != null ? currency : "USD";

        // Set defaults for null values
        totalEarned = totalEarned != null ? totalEarned : BigDecimal.ZERO;
        totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
        pending = pending != null ? pending : BigDecimal.ZERO;
    }
}
