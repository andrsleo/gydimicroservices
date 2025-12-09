package com.affiliate.rentals.gydi.bookings.domain.port.in;

/**
 * Input Port for updating booking status.
 * <p>
 * Handles transitions: REQUEST → RESERVED → FINISHED → CANCELED
 * </p>
 */
public interface UpdateBookingStatusUseCase {

    /**
     * Updates the booking status.
     *
     * @param command the status update command
     * @return the updated booking response
     */
    CreateBookingUseCase.BookingResponse execute(UpdateStatusCommand command);

    /**
     * Command to update booking status.
     * Cancellation details (reason, who canceled) removed - not tracked in booking domain anymore.
     */
    record UpdateStatusCommand(
        Long bookingId,
        String targetStatus // RESERVED, FINISHED, CANCELED
    ) {
        public UpdateStatusCommand {
            if (bookingId == null) {
                throw new IllegalArgumentException("Booking ID is required");
            }
            if (targetStatus == null || targetStatus.isBlank()) {
                throw new IllegalArgumentException("Target status is required");
            }
        }
    }
}
