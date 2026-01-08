package com.affiliate.rentals.gydi.subscriptions.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting an invalid plan transition.
 *
 * <p>For example, trying to "upgrade" from PRO to FREE, or changing to the same plan.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.BAD_REQUEST, errorType = "Invalid Plan Transition")
public final class InvalidPlanTransitionException extends SubscriptionDomainException {

    public InvalidPlanTransitionException(String message) {
        super(message);
    }

    public static InvalidPlanTransitionException samePlan(String planCode) {
        return new InvalidPlanTransitionException(
                "User is already subscribed to plan: " + planCode);
    }

    public static InvalidPlanTransitionException transition(String fromPlan, String toPlan, String reason) {
        return new InvalidPlanTransitionException(
                String.format("Cannot change from %s to %s: %s", fromPlan, toPlan, reason));
    }
}
