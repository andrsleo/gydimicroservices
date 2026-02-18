package com.affiliate.rentals.gydi.commissions.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for host commission data transfer.
 */
public record HostCommissionDto(
    Long id,
    Long bookingId,
    Long hostId,
    String hostPlan,
    BigDecimal bookingAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    String currency,
    String status,
    String stripePaymentIntentId,
    String stripeChargeId,
    LocalDateTime chargedAt,
    String failureReason,
    Integer attemptCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
