package com.affiliate.rentals.gydi.commissions.application.dto;

/**
 * DTO for Stripe Connect onboarding link response.
 *
 * @param url The onboarding URL to redirect user to
 * @param stripeAccountId The Stripe Connect Account ID
 * @param expiresAt Expiration timestamp (23 hours from creation)
 */
public record OnboardingLinkDto(
    String url,
    String stripeAccountId,
    String expiresAt
) {
    public static OnboardingLinkDto of(String url, String stripeAccountId) {
        // Stripe onboarding links expire after 23 hours
        String expiresAt = java.time.LocalDateTime.now()
            .plusHours(23)
            .toString();
        return new OnboardingLinkDto(url, stripeAccountId, expiresAt);
    }
}
