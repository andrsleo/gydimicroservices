package com.affiliate.rentals.gydi.commissions.domain.ports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Port interface for Stripe payment gateway integration.
 * <p>
 * Handles HOST commission charges only. Affiliate payouts are handled by
 * {@link PayoutGatewayPort} (PayPal Payouts API).
 * </p>
 * <p>
 * Provides methods for:
 * <ul>
 *   <li>HOST commission charges (Payment Intents with off_session=true)</li>
 *   <li>Payment method verification</li>
 * </ul>
 * </p>
 * <p>
 * Note: Stripe Connect affiliate payout methods (createConnectAccount,
 * generateConnectOnboardingLink, transferToAffiliate, createLoginLink,
 * getConnectAccountStatus) have been removed. GYDI now uses PayPal Payouts
 * for affiliate commission payments.
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
     * @param customerId      Stripe customer ID of the host
     * @param paymentMethodId Stripe payment method ID to charge
     * @param amountCents     amount to charge in cents (e.g., 2000 = $20.00)
     * @param currency        currency code (USD, EUR, GBP, etc.)
     * @param bookingId       booking ID for metadata tracking
     * @param commissionId    commission ID for metadata tracking
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
     * @param hostConnectAccountId  Stripe Connect account ID of host (acct_xxx)
     * @param customerId            Stripe platform customer ID of host (cus_xxx) for off-session charge
     * @param paymentMethodId       Stripe payment method ID to charge
     * @param totalAmountCents      full booking amount in cents (charged to customer)
     * @param applicationFeeCents   platform commission in cents (kept by platform)
     * @param currency              currency code (USD, EUR, GBP, etc.)
     * @param bookingId             booking ID for metadata tracking
     * @param commissionId          commission ID for metadata tracking
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
     * @param chargeId    Stripe charge ID to refund
     * @param amountCents refund amount in cents
     * @param reason      refund reason
     * @return PaymentResult with refund status
     */
    PaymentResult refundHostCommission(
        String chargeId,
        Long amountCents,
        String reason
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
     * @param customerId      Stripe customer ID
     * @param paymentMethodId Stripe payment method ID to verify
     * @return true if verification succeeds, false otherwise
     */
    boolean verifyPaymentMethod(String customerId, String paymentMethodId);

    // ============================================================================
    // RESULT RECORDS
    // ============================================================================

    /**
     * Payment result record for host charges.
     */
    record PaymentResult(
        boolean success,
        String transactionId, // Payment Intent ID
        String chargeId,      // Stripe Charge ID (for host commissions)
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
}
