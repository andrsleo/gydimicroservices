package com.affiliate.rentals.gydi.subscriptions.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for canceling a subscription.
 *
 * <p>This record encapsulates the data required to cancel a user's subscription.
 * A reason must be provided for analytics and customer retention purposes.</p>
 *
 * @param reason the reason for cancellation (required for analytics)
 * @param cancelImmediately if true, cancels immediately; if false, at period end
 *
 * @author GYDI Development Team
 */
public record CancelSubscriptionRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @NotNull(message = "cancelImmediately flag is required")
        Boolean cancelImmediately
) {
    /**
     * Constructor with default value for cancelImmediately.
     * By default, cancels at the end of the billing period.
     */
    public CancelSubscriptionRequest(String reason) {
        this(reason, false);
    }
}
