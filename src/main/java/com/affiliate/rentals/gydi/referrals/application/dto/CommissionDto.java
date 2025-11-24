package com.affiliate.rentals.gydi.referrals.application.dto;

import com.affiliate.rentals.gydi.referrals.domain.model.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de comisión
 */
public record CommissionDto(
        Long id,
        Long referralLinkId,
        Long affiliateId,
        Long propertyId,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        String affiliatePlan,
        CommissionStatus status,
        Long remainingHoldDays,
        boolean isReadyForApproval,
        boolean isPayable,
        LocalDateTime createdAt) {
}