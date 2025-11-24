package com.affiliate.rentals.gydi.referrals.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO para generar un nuevo enlace de referido
 *
 * SECURITY: affiliateId is NOT included in request - it's extracted from JWT token
 * server-side to prevent IDOR attacks. Client cannot manipulate the affiliate ID.
 */
public record GenerateReferralLinkRequest(

        @NotNull(message = "PropertyId is required") Long propertyId,

        @Positive(message = "Expiration days must be positive") Integer expirationDays // Opcional, por defecto 90 días
) {
    // Constructor compacto con validaciones
    public GenerateReferralLinkRequest {
        if (expirationDays == null) {
            expirationDays = 90; // Default: 90 días
        }
        if (expirationDays <= 0 || expirationDays > 365) {
            throw new IllegalArgumentException("Expiration days must be between 1 and 365");
        }
    }
}