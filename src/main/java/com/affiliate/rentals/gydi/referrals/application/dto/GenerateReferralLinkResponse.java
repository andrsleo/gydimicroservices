package com.affiliate.rentals.gydi.referrals.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO para el enlace de referido generado
 */
public record GenerateReferralLinkResponse(
        Long id,
        String encryptedToken,
        String shortCode,
        String fullUrl, // URL completa: https://gydi.com/ref/{shortCode}
        String qrCodeUrl, // URL para generar QR code
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {
}