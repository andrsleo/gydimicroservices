package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested payment method cannot be found.
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "Payment Method Not Found")
public final class PaymentMethodNotFoundException extends SubscriptionDomainException {

    private static final String DEFAULT_MESSAGE = "Payment method not found";

    public PaymentMethodNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public PaymentMethodNotFoundException(String message) {
        super(message);
    }

    public static PaymentMethodNotFoundException withId(Long paymentMethodId) {
        return new PaymentMethodNotFoundException("Payment method not found with ID: " + paymentMethodId);
    }

    public static PaymentMethodNotFoundException defaultFor(Long userId) {
        return new PaymentMethodNotFoundException("No default payment method found for user ID: " + userId);
    }
}
