package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Thrown when a user attempts to access or modify a property they do not own.
 */
public class UnauthorizedPropertyAccessException extends RuntimeException {

    public UnauthorizedPropertyAccessException(String message) {
        super(message);
    }

    public UnauthorizedPropertyAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
