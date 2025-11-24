package com.affiliate.rentals.gydi.referrals.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO para rastrear un click en un enlace de referido
 */
public record TrackClickRequest(

        @NotNull(message = "ReferralLinkId is required") Long referralLinkId,

        @NotBlank(message = "IP address is required") String ipAddress,

        @NotBlank(message = "User agent is required") String userAgent,

        String fingerprint, // Fingerprint del browser (opcional)
        String countryCode, // Código ISO del país (opcional)
        String referer // URL de referencia (opcional)
) {
}