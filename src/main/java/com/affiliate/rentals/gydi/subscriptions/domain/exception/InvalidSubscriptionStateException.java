package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a subscription operation is attempted on a subscription
 * in an invalid state.
 *
 * <p>For example, trying to cancel an already canceled subscription, or trying
 * to renew a subscription that is not expired.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.BAD_REQUEST, errorType = "Invalid Subscription State")
public final class InvalidSubscriptionStateException extends SubscriptionDomainException {

    public InvalidSubscriptionStateException(String message) {
        super(message);
    }

    public static InvalidSubscriptionStateException cannotCancel(String currentStatus) {
        return new InvalidSubscriptionStateException(
                "Cannot cancel subscription with current status: " + currentStatus);
    }

    public static InvalidSubscriptionStateException cannotRenew(String currentStatus) {
        return new InvalidSubscriptionStateException(
                "Cannot renew subscription with current status: " + currentStatus);
    }

    public static InvalidSubscriptionStateException alreadySubscribed(String planCode) {
        return new InvalidSubscriptionStateException(
                "User is already subscribed to plan: " + planCode);
    }
}
