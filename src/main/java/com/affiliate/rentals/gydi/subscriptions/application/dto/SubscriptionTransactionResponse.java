package com.affiliate.rentals.gydi.subscriptions.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for subscription transaction responses.
 *
 * <p>This record encapsulates transaction information returned to clients.
 * It includes payment details, status, and any failure reasons.</p>
 *
 * @param id the unique identifier of the transaction
 * @param userSubscriptionId the ID of the related subscription
 * @param transactionType the type of transaction (UPGRADE, RENEWAL, etc.)
 * @param transactionStatus the status (PENDING, COMPLETED, FAILED, etc.)
 * @param fromPlanId the ID of the plan being changed from (for plan changes)
 * @param fromPlanName the name of the plan being changed from
 * @param toPlanId the ID of the plan being changed to (for plan changes)
 * @param toPlanName the name of the plan being changed to
 * @param amount the transaction amount
 * @param currency the currency code (e.g., "USD")
 * @param stripePaymentIntentId the Stripe Payment Intent ID
 * @param stripeChargeId the Stripe Charge ID (after confirmation)
 * @param failureReason the reason for failure (if transaction failed)
 * @param processedAt when the transaction was processed
 * @param createdAt timestamp when the transaction was created
 *
 * @author GYDI Development Team
 */
public record SubscriptionTransactionResponse(
        Long id,
        Long userSubscriptionId,
        String transactionType,
        String transactionStatus,
        Long fromPlanId,
        String fromPlanName,
        Long toPlanId,
        String toPlanName,
        BigDecimal amount,
        String currency,
        String stripePaymentIntentId,
        String stripeChargeId,
        String failureReason,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    /**
     * Note: gatewayResponse is intentionally NOT included in the response
     * to avoid leaking sensitive payment gateway data.
     */
}
