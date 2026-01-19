package com.affiliate.rentals.gydi.subscriptions.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for changing (upgrading or downgrading) a subscription plan.
 *
 * <p>This record encapsulates the data required to change a user's subscription plan.
 * Can be used for both upgrades (e.g., FREE → PRO) and downgrades (e.g., PRO → FREE).</p>
 *
 * <p>When upgrading to a paid plan, the user's existing default payment method will be used.
 * If no payment method exists, the request will fail.</p>
 *
 * @param newPlanCode the code of the plan to change to (e.g., "PRO", "ELITE", "FREE")
 * @param paymentMethodId optional payment method ID (if different from default)
 *
 * @author GYDI Development Team
 */
public record ChangePlanRequest(
        @NotBlank(message = "New plan code is required")
        String newPlanCode,

        Long paymentMethodId
) {
}
