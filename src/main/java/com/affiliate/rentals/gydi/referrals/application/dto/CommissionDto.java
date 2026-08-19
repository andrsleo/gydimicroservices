package com.affiliate.rentals.gydi.referrals.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de comisión
 */
public record CommissionDto(
                Long id,
                Long bookingId,
                BigDecimal commissionRate,
                BigDecimal commissionAmount,
                String affiliatePlan,
                String status,
                Long remainingHoldDays,
                boolean isReadyForApproval,
                boolean isPayable,
                LocalDateTime createdAt) {
}