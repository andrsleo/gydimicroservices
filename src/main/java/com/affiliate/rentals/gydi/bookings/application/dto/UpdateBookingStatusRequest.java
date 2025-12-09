package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating booking status.
 * <p>
 * Supports transitions:
 * - REQUEST → RESERVED (host confirms)
 * - RESERVED → FINISHED (stay completed)
 * - REQUEST/RESERVED → CANCELED (booking canceled)
 * </p>
 * <p>
 * Cancellation details (reason, who canceled, timestamp) are not tracked in the booking table.
 * If needed, they can be logged separately in audit systems.
 * </p>
 */
public record UpdateBookingStatusRequest(

    @NotBlank(message = "Target status is required")
    @Pattern(
        regexp = "^(RESERVED|FINISHED|CANCELED)$",
        message = "Target status must be RESERVED, FINISHED, or CANCELED"
    )
    String targetStatus
) {
}
