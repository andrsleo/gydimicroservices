package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Thrown when a calendar operation is not allowed.
 * <p>
 * Examples:
 * - Attempting to unblock a date that is blocked by an active booking
 * - Blocking a date that is already booked
 * </p>
 */
public class InvalidCalendarOperationException extends RuntimeException {

    public InvalidCalendarOperationException(String message) {
        super(message);
    }

    public InvalidCalendarOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
