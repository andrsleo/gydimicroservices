package com.affiliate.rentals.gydi.commissions.application.dto;

/**
 * DTO for Stripe Connect Account status.
 *
 * @param hasAccount Whether user has a Connect Account
 * @param onboardingCompleted Whether onboarding is complete
 * @param payoutsEnabled Whether payouts are enabled
 * @param verificationStatus Verification status (unverified, pending, verified, disabled)
 * @param stripeAccountId The Stripe Account ID (if exists)
 */
public record ConnectAccountStatusDto(
    boolean hasAccount,
    boolean onboardingCompleted,
    boolean payoutsEnabled,
    String verificationStatus,
    String stripeAccountId
) {
    public static ConnectAccountStatusDto notFound() {
        return new ConnectAccountStatusDto(false, false, false, "unverified", null);
    }
}
