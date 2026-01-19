package com.affiliate.rentals.gydi.subscriptions.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new payment method.
 *
 * <p>This record encapsulates the data required to create a payment method.
 * The actual card details are processed by Stripe - we only store the tokenized
 * payment method ID returned by Stripe.</p>
 *
 * <p><b>SECURITY NOTE:</b> Never send raw card numbers to the backend.
 * Always tokenize payment details using Stripe Elements on the frontend first.</p>
 *
 * @param stripePaymentMethodId the tokenized payment method ID from Stripe
 * @param billingEmail the billing email address
 * @param isDefault whether this should be the default payment method
 *
 * @author GYDI Development Team
 */
public record CreatePaymentMethodRequest(
        @NotBlank(message = "Stripe payment method ID is required")
        String stripePaymentMethodId,

        @Email(message = "Billing email must be valid")
        String billingEmail,

        @NotNull(message = "isDefault flag is required")
        Boolean isDefault
) {
    /**
     * Constructor with default value for isDefault.
     */
    public CreatePaymentMethodRequest(String stripePaymentMethodId, String billingEmail) {
        this(stripePaymentMethodId, billingEmail, false);
    }
}
