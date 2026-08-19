package com.affiliate.rentals.gydi.referrals.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO para generar un nuevo enlace de referido
 *
 * SECURITY: affiliateId is NOT included in request - it's extracted from JWT token
 * server-side to prevent IDOR attacks. Client cannot manipulate the affiliate ID.
 *
 * PHASE 1: Expiration is now calculated automatically based on user's subscription plan:
 * - FREE: 30 days
 * - PRO: 90 days
 * - ELITE: 365 days
 */
public record GenerateReferralLinkRequest(
        @NotNull(message = "PropertyId is required") Long propertyId
) {
}