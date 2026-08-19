package com.affiliate.rentals.gydi.bookings.domain.exception;

/**
 * Exception thrown when a user attempts to access a booking they do not own.
 * <p>
 * This exception represents a security violation (Broken Access Control / IDOR).
 * It should result in an HTTP 403 Forbidden response.
 * </p>
 * <p>
 * <strong>Security Note:</strong> The exception message should NOT reveal
 * whether the booking exists or not (to prevent information disclosure).
 * </p>
 *
 * @author GYDI Development Team
 */
public class BookingAccessDeniedException extends RuntimeException {

    private final Long bookingId;
    private final Long requestingUserId;

    /**
     * Creates a new BookingAccessDeniedException.
     *
     * @param message the exception message
     */
    public BookingAccessDeniedException(String message) {
        super(message);
        this.bookingId = null;
        this.requestingUserId = null;
    }

    /**
     * Creates a new BookingAccessDeniedException with detailed information.
     * <p>
     * <strong>WARNING:</strong> The detailed info (booking ID, user ID) should be
     * logged for auditing but NOT exposed to the client in the HTTP response.
     * </p>
     *
     * @param message the exception message
     * @param bookingId the booking ID that was accessed
     * @param requestingUserId the user ID who attempted access
     */
    public BookingAccessDeniedException(String message, Long bookingId, Long requestingUserId) {
        super(message);
        this.bookingId = bookingId;
        this.requestingUserId = requestingUserId;
    }

    /**
     * Returns the booking ID that was accessed (for audit logging).
     *
     * @return the booking ID, or null if not set
     */
    public Long getBookingId() {
        return bookingId;
    }

    /**
     * Returns the user ID who attempted access (for audit logging).
     *
     * @return the requesting user ID, or null if not set
     */
    public Long getRequestingUserId() {
        return requestingUserId;
    }
}
