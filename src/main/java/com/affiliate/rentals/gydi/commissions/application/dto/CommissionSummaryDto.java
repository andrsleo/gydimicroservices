package com.affiliate.rentals.gydi.commissions.application.dto;

import java.math.BigDecimal;

/**
 * DTO for commission summary (dashboard stats).
 */
public record CommissionSummaryDto(
    Long totalCommissions,
    BigDecimal totalAmount,
    BigDecimal pendingAmount,
    BigDecimal approvedAmount,
    BigDecimal paidAmount,
    String currency
) {}
