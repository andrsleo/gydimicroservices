package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment gateway operation fails.
 *
 * <p>This exception is part of the sealed {@link SubscriptionDomainException} hierarchy
 * and represents failures in communication with external payment providers (e.g., Stripe).</p>
 *
 * <p><b>Common Scenarios:</b></p>
 * <ul>
 *   <li>Payment method detachment fails</li>
 *   <li>Payment intent creation fails</li>
 *   <li>Gateway API is unavailable or returns errors</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.BAD_GATEWAY, errorType = "Payment Gateway Error")
public final class PaymentGatewayException extends SubscriptionDomainException {

    private static final String DEFAULT_MESSAGE = "Payment gateway operation failed";

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception for a failed payment method detachment.
     *
     * @param paymentMethodToken the payment method token that failed to detach
     * @param cause the underlying cause
     * @return a new PaymentGatewayException
     */
    public static PaymentGatewayException detachmentFailed(String paymentMethodToken, Throwable cause) {
        return new PaymentGatewayException(
            "Failed to detach payment method " + paymentMethodToken + " from payment gateway",
            cause
        );
    }

    /**
     * Creates a generic exception for payment gateway failures.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @return a new PaymentGatewayException
     */
    public static PaymentGatewayException fromCause(String message, Throwable cause) {
        return new PaymentGatewayException(message, cause);
    }
}
