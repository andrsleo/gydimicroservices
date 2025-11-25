package com.affiliate.rentals.gydi.bookings.domain.exception;

import java.time.LocalDate;

/**
 * Thrown when requested dates are not available for booking.
 * <p>
 * Reasons:
 * - Dates already reserved by another booking
 * - Dates in the past
 * - Property not available for those dates
 */
public class DateRangeNotAvailableException extends RuntimeException {

    public DateRangeNotAvailableException(LocalDate startDate, LocalDate endDate) {
        super(String.format(
            "Dates from %s to %s are not available for booking",
            startDate, endDate
        ));
    }

    public DateRangeNotAvailableException(String message) {
        super(message);
    }

    public DateRangeNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
