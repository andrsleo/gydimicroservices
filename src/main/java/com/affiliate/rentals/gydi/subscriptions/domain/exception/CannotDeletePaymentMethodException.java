package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment method cannot be deleted due to business rule violations.
 *
 * <p>This exception is thrown in scenarios such as:</p>
 * <ul>
 *   <li>Attempting to delete the only payment method while having an active paid subscription</li>
 *   <li>Attempting to delete the default payment method without setting another as default first</li>
 *   <li>Attempting to delete a payment method that doesn't belong to the user</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.BAD_REQUEST, errorType = "Cannot Delete Payment Method")
public final class CannotDeletePaymentMethodException extends SubscriptionDomainException {

    private static final String DEFAULT_MESSAGE = "Payment method cannot be deleted";

    public CannotDeletePaymentMethodException() {
        super(DEFAULT_MESSAGE);
    }

    public CannotDeletePaymentMethodException(String message) {
        super(message);
    }

    /**
     * Creates an exception for attempting to delete the only payment method on a paid subscription.
     *
     * @return exception with descriptive message
     */
    public static CannotDeletePaymentMethodException onlyMethodOnPaidSubscription() {
        return new CannotDeletePaymentMethodException(
            "Cannot delete the only payment method while you have an active paid subscription. " +
            "Please add another payment method first or cancel your subscription."
        );
    }

    /**
     * Creates an exception for attempting to delete the default payment method.
     *
     * @return exception with descriptive message
     */
    public static CannotDeletePaymentMethodException isDefaultMethod() {
        return new CannotDeletePaymentMethodException(
            "Cannot delete default payment method. Please set another payment method as default first."
        );
    }

    /**
     * Creates an exception for attempting to delete a payment method that doesn't belong to the user.
     *
     * @return exception with descriptive message
     */
    public static CannotDeletePaymentMethodException notOwnedByUser() {
        return new CannotDeletePaymentMethodException(
            "Payment method not found or does not belong to you."
        );
    }

    /**
     * Creates an exception with a custom reason.
     *
     * @param reason the reason why the payment method cannot be deleted
     * @return exception with the provided reason
     */
    public static CannotDeletePaymentMethodException withReason(String reason) {
        return new CannotDeletePaymentMethodException(reason);
    }
}
