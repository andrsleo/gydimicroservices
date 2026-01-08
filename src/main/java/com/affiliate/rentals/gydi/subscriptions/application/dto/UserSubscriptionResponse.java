package com.affiliate.rentals.gydi.subscriptions.application.dto;

import java.time.LocalDateTime;

/**
 * DTO for user subscription responses.
 *
 * <p>This record encapsulates user subscription information returned to clients.
 * It includes the current plan, status, and billing information.</p>
 *
 * @param id the unique identifier of the subscription
 * @param userId the ID of the user who owns this subscription
 * @param planId the ID of the subscription plan
 * @param planCode the plan code (e.g., "FREE", "PRO", "ELITE")
 * @param planName the plan display name
 * @param status the subscription status (ACTIVE, EXPIRED, CANCELED, SUSPENDED)
 * @param startedAt when the subscription started
 * @param expiresAt when the subscription expires (null for FREE plan)
 * @param canceledAt when the subscription was canceled
 * @param cancelationReason the reason for cancellation
 * @param autoRenew whether auto-renewal is enabled
 * @param lastRenewalAt the date of the last renewal
 * @param nextBillingDate the date of the next billing
 * @param stripeSubscriptionId the Stripe subscription ID
 * @param createdAt timestamp when the subscription was created
 * @param updatedAt timestamp when the subscription was last updated
 *
 * @author GYDI Development Team
 */
public record UserSubscriptionResponse(
        Long id,
        Long userId,
        Long planId,
        String planCode,
        String planName,
        String status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime canceledAt,
        String cancelationReason,
        Boolean autoRenew,
        LocalDateTime lastRenewalAt,
        LocalDateTime nextBillingDate,
        String stripeSubscriptionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
