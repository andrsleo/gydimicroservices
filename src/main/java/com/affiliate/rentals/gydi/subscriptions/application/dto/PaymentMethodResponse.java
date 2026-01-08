package com.affiliate.rentals.gydi.subscriptions.application.dto;

import java.time.LocalDateTime;

/**
 * DTO for payment method responses.
 *
 * <p>This record encapsulates payment method information returned to clients.
 * For security reasons, it only includes the last 4 digits of the card, never full numbers.</p>
 *
 * @param id the unique identifier of the payment method
 * @param userId the ID of the user who owns this payment method
 * @param methodType the payment method type (CREDIT_CARD, DEBIT_CARD, etc.)
 * @param cardLastFour the last 4 digits of the card (for display purposes)
 * @param cardBrand the card brand (Visa, Mastercard, Amex, etc.)
 * @param cardExpMonth the card expiration month (1-12)
 * @param cardExpYear the card expiration year
 * @param billingEmail the billing email address
 * @param isDefault whether this is the default payment method
 * @param isActive whether this payment method is active
 * @param isExpired whether the card has expired
 * @param displayName a user-friendly display name (e.g., "Visa ending in 4242")
 * @param createdAt timestamp when the payment method was created
 *
 * @author GYDI Development Team
 */
public record PaymentMethodResponse(
        Long id,
        Long userId,
        String methodType,
        String cardLastFour,
        String cardBrand,
        Integer cardExpMonth,
        Integer cardExpYear,
        String billingEmail,
        Boolean isDefault,
        Boolean isActive,
        Boolean isExpired,
        String displayName,
        LocalDateTime createdAt
) {
    /**
     * Note: gatewayToken is intentionally NOT included in the response for security reasons.
     * Token should never be sent to the client.
     */
}
