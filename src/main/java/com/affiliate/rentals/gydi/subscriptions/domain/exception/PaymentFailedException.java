package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment operation fails.
 *
 * <p>This can occur due to insufficient funds, declined card, network issues,
 * or any other payment gateway error.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.PAYMENT_REQUIRED, errorType = "Payment Failed")
public final class PaymentFailedException extends SubscriptionDomainException {

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PaymentFailedException withReason(String reason) {
        return new PaymentFailedException("Payment failed: " + reason);
    }

    public static PaymentFailedException forPlan(String planCode, String reason) {
        return new PaymentFailedException(
                "Payment failed for plan " + planCode + ": " + reason);
    }
}
