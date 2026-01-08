package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested user subscription cannot be found.
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "Subscription Not Found")
public final class SubscriptionNotFoundException extends SubscriptionDomainException {

    private static final String DEFAULT_MESSAGE = "User subscription not found";

    public SubscriptionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }

    public static SubscriptionNotFoundException forUser(Long userId) {
        return new SubscriptionNotFoundException("No subscription found for user ID: " + userId);
    }

    public static SubscriptionNotFoundException withId(Long subscriptionId) {
        return new SubscriptionNotFoundException("Subscription not found with ID: " + subscriptionId);
    }
}
