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
     */
    record UpdateStatusCommand(
        Long bookingId,
        String targetStatus, // RESERVED, FINISHED, CANCELED
        String cancellationReason, // Required if targetStatus = CANCELED
        String canceledBy // Required if targetStatus = CANCELED (CLIENT, OWNER, ADMIN, SYSTEM)
    ) {
        public UpdateStatusCommand {
            if (bookingId == null) {
                throw new IllegalArgumentException("Booking ID is required");
            }
            if (targetStatus == null || targetStatus.isBlank()) {
                throw new IllegalArgumentException("Target status is required");
            }

            // Validate that if CANCELED, we have reason and canceledBy
            if ("CANCELED".equals(targetStatus)) {
                if (cancellationReason == null || cancellationReason.isBlank()) {
                    throw new IllegalArgumentException(
                        "Cancellation reason is required when canceling a booking"
                    );
                }
                if (canceledBy == null || canceledBy.isBlank()) {
                    throw new IllegalArgumentException(
                        "Canceled by is required when canceling a booking"
                    );
                }
            }
        }
    }
}
