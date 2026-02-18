package com.affiliate.rentals.gydi.commissions.domain.exception;

/**
 * Exception thrown when user already completed Stripe Connect onboarding.
 */
public class AlreadyOnboardedException extends RuntimeException {

    public AlreadyOnboardedException(String message) {
        super(message);
    }

    public AlreadyOnboardedException(String message, Throwable cause) {
        super(message, cause);
    }
}
