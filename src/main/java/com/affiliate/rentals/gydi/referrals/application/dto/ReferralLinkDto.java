package com.affiliate.rentals.gydi.referrals.application.dto;

import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLinkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de enlace de referido
 */
public record ReferralLinkDto(
        Long id,
        Long affiliateId,
        Long propertyId,
        String shortCode,
        String fullUrl,
        Integer clicksCount,
        ReferralLinkStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}