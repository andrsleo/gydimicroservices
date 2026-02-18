package com.affiliate.rentals.gydi.referrals.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO para la renovación de un enlace de referido
 *
 * FASE 2: Renovación manual de links
 * Retorna la información actualizada del link renovado
 */
public record RenewReferralLinkResponse(
        Long id,
        String encryptedToken,
        String shortCode,
        String fullUrl,
        String qrCodeUrl,
        LocalDateTime expiresAt,
        LocalDateTime renewedAt
) {
}
