package com.affiliate.rentals.gydi.subscriptions.domain.model;

/**
 * Enumeration of supported payment method types.
 *
 * <p>This enum represents the different payment methods that users can use
 * to pay for subscriptions.</p>
 *
 * @author GYDI Development Team
 */
public enum PaymentMethodType {
    /**
     * Credit card payment method.
     */
    CREDIT_CARD,

    /**
     * Debit card payment method.
     */
    DEBIT_CARD,

    /**
     * PayPal payment method.
     */
    PAYPAL,

    /**
     * Stripe payment method (includes various card types processed through Stripe).
     */
    STRIPE,

    /**
     * Other payment methods not explicitly categorized.
     */
    OTHER;

    /**
     * Checks if the payment method is a card type.
     *
     * @return {@code true} if payment method is a card, {@code false} otherwise
     */
    public boolean isCard() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }

    /**
     * Checks if the payment method requires tokenization.
     *
     * @return {@code true} if tokenization is required, {@code false} otherwise
     */
    public boolean requiresTokenization() {
        return isCard() || this == STRIPE;
    }

    /**
     * Returns a user-friendly display name for the payment method.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return switch (this) {
            case CREDIT_CARD -> "Credit Card";
            case DEBIT_CARD -> "Debit Card";
            case PAYPAL -> "PayPal";
            case STRIPE -> "Stripe";
            case OTHER -> "Other";
        };
    }
}
