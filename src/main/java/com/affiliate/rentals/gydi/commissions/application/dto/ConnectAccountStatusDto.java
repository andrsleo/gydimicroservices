package com.affiliate.rentals.gydi.commissions.application.dto;

/**
 * DTO for affiliate payout account status.
 *
 * @param hasAccount             Whether user has an account record
 * @param onboardingCompleted    Legacy Stripe Connect field — kept for backward compatibility
 * @param payoutsEnabled         Legacy Stripe Connect field — kept for backward compatibility
 * @param verificationStatus     Account verification status (unverified, pending, verified, disabled)
 * @param stripeAccountId        Legacy Stripe Connect Account ID (if exists)
 * @param paypalEmail            The affiliate's registered PayPal email for payouts (null if not set)
 * @param paypalEmailConfigured  True if the affiliate has registered a PayPal email
 */
public record ConnectAccountStatusDto(
    boolean hasAccount,
    boolean onboardingCompleted,
    boolean payoutsEnabled,
    String verificationStatus,
    String stripeAccountId,
    String paypalEmail,
    boolean paypalEmailConfigured
) {
    public static ConnectAccountStatusDto notFound() {
        return new ConnectAccountStatusDto(false, false, false, "unverified", null, null, false);
    }

    /**
     * Factory method for accounts without PayPal email configured.
     */
    public static ConnectAccountStatusDto withoutPayPal(
            boolean onboardingCompleted,
            boolean payoutsEnabled,
            String verificationStatus,
            String stripeAccountId
    ) {
        return new ConnectAccountStatusDto(
                true, onboardingCompleted, payoutsEnabled,
                verificationStatus, stripeAccountId, null, false
        );
    }

    /**
     * Factory method for accounts with PayPal email configured.
     */
    public static ConnectAccountStatusDto withPayPal(
            boolean onboardingCompleted,
            boolean payoutsEnabled,
            String verificationStatus,
            String stripeAccountId,
            String paypalEmail
    ) {
        return new ConnectAccountStatusDto(
                true, onboardingCompleted, payoutsEnabled,
                verificationStatus, stripeAccountId, paypalEmail, true
        );
    }
}
