package com.affiliate.rentals.gydi.commissions.domain.ports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Port interface for affiliate commission payout gateway (PayPal Payouts API).
 * <p>
 * Provides a single method to send a payout to an affiliate via their PayPal email.
 * This port is intentionally narrow — it only covers affiliate payouts.
 * Host commission charging remains in {@link PaymentGatewayPort} (Stripe).
 * </p>
 * <p>
 * Implemented by:
 * <ul>
 *   <li>{@code PayPalPayoutAdapter} (prod profile) — calls PayPal Payouts API</li>
 *   <li>{@code DevPayoutAdapter} (non-prod) — logs and returns mock success</li>
 * </ul>
 * </p>
 *
 * <p><strong>PURE DOMAIN PORT</strong> — no framework dependencies.</p>
 */
public interface PayoutGatewayPort {

    /**
     * Sends a payout to an affiliate via PayPal.
     * <p>
     * Creates a single-item PayPal Batch Payout. The call is synchronous from
     * the caller's perspective but PayPal processes payouts asynchronously.
     * Funds typically arrive within minutes for verified PayPal accounts.
     * </p>
     *
     * @param recipientPayPalEmail  the affiliate's PayPal email address
     * @param amount                payout amount (e.g., 12.50)
     * @param currency              ISO 4217 currency code (e.g., "USD")
     * @param referenceId           unique reference for idempotency (e.g., commission ID)
     * @param note                  note shown to recipient in PayPal (e.g., "Comision GYDI reserva #123")
     * @return {@link PayoutResult} with batch/item IDs and status
     */
    PayoutResult sendPayout(
        String recipientPayPalEmail,
        BigDecimal amount,
        String currency,
        String referenceId,
        String note
    );

    // ============================================================================
    // RESULT RECORD
    // ============================================================================

    /**
     * Result of a PayPal payout request.
     *
     * @param success          true if PayPal accepted the payout request
     * @param payoutBatchId    PayPal Batch Payout ID (e.g., "PAYOUT-xxxxxxxxxxxx")
     * @param payoutItemId     PayPal individual payout item ID within the batch
     * @param status           PayPal payout status (PENDING, PROCESSING, SUCCESS, FAILED, etc.)
     * @param failureReason    human-readable failure reason (null on success)
     * @param processedAt      timestamp when the request was submitted
     */
    record PayoutResult(
        boolean success,
        String payoutBatchId,
        String payoutItemId,
        String status,
        String failureReason,
        LocalDateTime processedAt
    ) {
        public static PayoutResult success(String payoutBatchId, String payoutItemId, String status) {
            return new PayoutResult(true, payoutBatchId, payoutItemId, status, null, LocalDateTime.now());
        }

        public static PayoutResult failure(String reason) {
            return new PayoutResult(false, null, null, "FAILED", reason, LocalDateTime.now());
        }
    }
}
