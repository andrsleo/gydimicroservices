package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Thrown when a booking is not found by its ID.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long bookingId) {
        super(String.format("Booking with ID %d not found", bookingId));
    }

    public BookingNotFoundException(String message) {
        super(message);
    }

    public BookingNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
