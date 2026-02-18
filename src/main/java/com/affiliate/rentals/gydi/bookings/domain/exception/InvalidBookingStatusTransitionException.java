package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Thrown when an invalid booking status transition is attempted.
 * <p>
 * Domain exception - no framework dependencies.
 * </p>
 */
public class InvalidBookingStatusTransitionException extends RuntimeException {

    public InvalidBookingStatusTransitionException(String message) {
        super(message);
    }

    public InvalidBookingStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
