package com.affiliate.rentals.gydi.referrals.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveReferralRequest(
        @NotBlank(message = "Token is required") String token,
        String ipAddress,
        String userAgent,
        String fingerprint,
        String countryCode,
        String referer) {
}
