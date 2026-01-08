package com.affiliate.rentals.gydi.subscriptions.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for subscribing to a subscription plan.
 *
 * <p>This record encapsulates the data required to subscribe a user to a plan.
 * For paid plans (PRO, ELITE), a payment method ID is required.
 * For the FREE plan, no payment method is needed.</p>
 *
 * @param planCode the code of the plan to subscribe to (e.g., "FREE", "PRO", "ELITE")
 * @param paymentMethodId the ID of the payment method (required for paid plans)
 * @param autoRenew whether to enable auto-renewal for the subscription
 *
 * @author GYDI Development Team
 */
public record SubscribeToPlanRequest(
        @NotBlank(message = "Plan code is required")
        String planCode,

        Long paymentMethodId,

        @NotNull(message = "Auto-renew flag is required")
        Boolean autoRenew
) {
    /**
     * Constructor with default value for autoRenew.
     */
    public SubscribeToPlanRequest(String planCode, Long paymentMethodId) {
        this(planCode, paymentMethodId, true);
    }
}
