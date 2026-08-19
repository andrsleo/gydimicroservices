package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested subscription plan cannot be found.
 *
 * <p>This exception is part of the sealed {@link SubscriptionDomainException} hierarchy
 * and represents a business rule violation where a plan operation is attempted
 * on a non-existent plan.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "Plan Not Found")
public final class PlanNotFoundException extends SubscriptionDomainException {

    private static final String DEFAULT_MESSAGE = "Subscription plan not found";

    public PlanNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public PlanNotFoundException(String message) {
        super(message);
    }

    public static PlanNotFoundException withId(Long planId) {
        return new PlanNotFoundException("Plan not found with ID: " + planId);
    }

    public static PlanNotFoundException withCode(String planCode) {
        return new PlanNotFoundException("Plan not found with code: " + planCode);
    }
}
