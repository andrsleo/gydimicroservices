package com.affiliate.rentals.gydi.commissions.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for referral commission data transfer.
 */
public record ReferralCommissionDto(
    Long id,
    Long bookingId,
    Long affiliateId,
    String affiliatePlan,
    BigDecimal bookingAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    String currency,
    LocalDate scheduledPaymentDate,
    LocalDateTime disputePeriodEndsAt,
    String status,
    String stripeTransferId,
    LocalDateTime paidAt,
    String failureReason,
    Integer attemptCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
