package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Exception thrown when a property is not available for booking.
 * <p>
 * Reasons:
 * - Property is not published
 * - Property has overlapping bookings for requested dates
 * </p>
 *
 * ✅ SECURITY FIX: Prevents invalid bookings.
 *
 * @author GYDI Security Team
 */
public class PropertyNotAvailableException extends RuntimeException {

    public PropertyNotAvailableException(String message) {
        super(message);
    }

    public PropertyNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
