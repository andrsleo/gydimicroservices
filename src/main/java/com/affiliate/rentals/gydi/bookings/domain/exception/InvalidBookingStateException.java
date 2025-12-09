package com.affiliate.rentals.gydi.bookings.domain.exception;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;

/**
 * Thrown when attempting an invalid booking state transition.
 * <p>
 * Example: Trying to finish a booking that is still in REQUEST status.
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(BookingStatus current, BookingStatus attempted) {
        super(String.format(
            "Cannot transition from %s to %s. Invalid state transition.",
            current, attempted
        ));
    }

    public InvalidBookingStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
