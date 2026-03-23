package com.affiliate.rentals.gydi.commissions.domain.ports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Port interface for payment gateway integration (Stripe).
 * <p>
 * Provides methods for:
 * - HOST commission charges (Payment Intents with off_session=true)
 * - AFFILIATE payouts (Stripe Connect Transfers)
 * - Stripe Connect account onboarding
 * - Payment method verification
 * </p>
 * <p>
 * Implemented by infrastructure layer Stripe adapter.
 * </p>
 */
public interface PaymentGatewayPort {

    // ============================================================================
    // HOST COMMISSION CHARGES (Payment Intents)
    // ============================================================================

    /**
     * Charges host commission via Stripe Payment Intent (off_session=true).
     * <p>
     * Uses the host's default payment method (from subscriptions or dedicated for commissions).
     * Payment is confirmed immediately without user interaction.
     * </p>
     *
     * @param customerId Stripe customer ID of the host
     * @param paymentMethodId Stripe payment method ID to charge
     * @param amountCents amount to charge in cents (e.g., 2000 = $20.00)
     * @param currency currency code (USD, EUR, GBP, etc.)
     * @param bookingId booking ID for metadata tracking
     * @param commissionId commission ID for metadata tracking
     * @return PaymentResult with success/failure and transaction IDs
     */
    PaymentResult chargeHostCommission(
        String customerId,
        String paymentMethodId,
        Long amountCents,
        String currency,
        String bookingId,
        String commissionId
    );

    /**
     * Charges host commission via Stripe Connect PaymentIntent.
     * <p>
     * Uses on_behalf_of and application_fee_amount for full Connect model:
     * - The charge is made on the host's Connect account (acct_xxx)
     * - The platform takes application_fee_amount (the commission)
     * - The host receives the remainder directly in their Connect account
     * </p>
     *
     * @param hostConnectAccountId Stripe Connect account ID of host (acct_xxx)
     * @param customerId Stripe platform customer ID of host (cus_xxx) for off-session charge
     * @param paymentMethodId Stripe payment method ID to charge
     * @param totalAmountCents full booking amount in cents (charged to customer)
     * @param applicationFeeCents platform commission in cents (kept by platform)
     * @param currency currency code (USD, EUR, GBP, etc.)
     * @param bookingId booking ID for metadata tracking
     * @param commissionId commission ID for metadata tracking
     * @return PaymentResult with success/failure and transaction IDs
     */
    PaymentResult chargeHostViaConnect(
        String hostConnectAccountId,
        String customerId,
        String paymentMethodId,
        Long totalAmountCents,
        Long applicationFeeCents,
        String currency,
        String bookingId,
        String commissionId
    );

    /**
     * Refunds a host commission charge.
     *
     * @param chargeId Stripe charge ID to refund
     * @param amountCents refund amount in cents
     * @param reason refund reason
     * @return PaymentResult with refund status
     */
    PaymentResult refundHostCommission(
        String chargeId,
        Long amountCents,
        String reason
    );

    // ============================================================================
    // AFFILIATE PAYOUTS (Stripe Connect)
    // ============================================================================

    /**
     * Creates a Stripe Connect account for an affiliate.
     * <p>
     * Account type varies by country:
     * - US, CA, UK: Standard
     * - EU countries: Express
     * - Other: Custom
     * </p>
     *
     * @param email affiliate's email
     * @param country ISO 3166-1 alpha-2 country code (e.g., "US", "MX")
     * @return Stripe Connect account ID (e.g., "acct_xxxxx")
     */
    String createConnectAccount(String email, String country);

    /**
     * Generates onboarding link for Stripe Connect account.
     * <p>
     * Affiliate is redirected to Stripe to complete KYC, bank account setup, etc.
     * </p>
     *
     * @param accountId Stripe Connect account ID
     * @param refreshUrl URL to redirect if onboarding is incomplete
     * @param returnUrl URL to redirect after successful onboarding
     * @return Onboarding link URL (expires after ~30 minutes)
     */
    String generateConnectOnboardingLink(
        String accountId,
        String refreshUrl,
        String returnUrl
    );

    /**
     * Transfers affiliate commission via Stripe Connect.
     * <p>
     * Creates a Transfer to the affiliate's Connect account.
     * Actual payout to affiliate's bank account happens per their payout schedule (default: 7 days).
     * </p>
     *
     * @param connectAccountId Stripe Connect account ID of affiliate
     * @param amountCents amount to transfer in cents
     * @param currency currency code
     * @param commissionIds comma-separated commission IDs (for metadata)
     * @return PaymentResult with success/failure and transfer ID
     */
    PaymentResult transferToAffiliate(
        String connectAccountId,
        Long amountCents,
        String currency,
        String commissionIds
    );

    // ============================================================================
    // PAYMENT METHOD VERIFICATION
    // ============================================================================

    /**
     * Verifies a payment method by creating a $1 authorization (not captured).
     * <p>
     * Used to validate that a payment method is active and can be charged.
     * The $1 authorization is released automatically.
     * </p>
     *
     * @param customerId Stripe customer ID
     * @param paymentMethodId Stripe payment method ID to verify
     * @return true if verification succeeds, false otherwise
     */
    boolean verifyPaymentMethod(String customerId, String paymentMethodId);

    // ============================================================================
    // CONNECT ACCOUNT QUERIES
    // ============================================================================

    /**
     * Retrieves the current status of a Stripe Connect account.
     * <p>
     * Used to check if onboarding is complete and payouts are enabled.
     * </p>
     *
     * @param accountId Stripe Connect account ID
     * @return ConnectAccountStatus with onboarding and payout status
     */
    ConnectAccountStatus getConnectAccountStatus(String accountId);

    /**
     * Creates a Stripe Express Dashboard login link for a connected account.
     * <p>
     * Allows users to access their Stripe Express Dashboard to manage bank accounts,
     * view payouts, and review transaction history.
     * </p>
     *
     * @param accountId Stripe Connect account ID (acct_xxx)
     * @return URL to redirect the user to their Stripe Express Dashboard
     */
    String createLoginLink(String accountId);

    // ============================================================================
    // RESULT RECORDS
    // ============================================================================

    /**
     * Payment result record for host charges and affiliate transfers.
     */
    record PaymentResult(
        boolean success,
        String transactionId, // Payment Intent ID or Transfer ID
        String chargeId, // Stripe Charge ID (for host commissions, null for transfers)
        String failureReason,
        LocalDateTime processedAt
    ) {
        public static PaymentResult success(String transactionId, String chargeId) {
            return new PaymentResult(true, transactionId, chargeId, null, LocalDateTime.now());
        }

        public static PaymentResult failure(String reason) {
            return new PaymentResult(false, null, null, reason, LocalDateTime.now());
        }
    }

    /**
     * Stripe Connect account status record.
     */
    record ConnectAccountStatus(
        String accountId,
        boolean onboardingCompleted,
        boolean payoutsEnabled,
        boolean chargesEnabled,
        String disabledReason
    ) {
        public boolean canReceivePayouts() {
            return onboardingCompleted && payoutsEnabled;
        }
    }
}
