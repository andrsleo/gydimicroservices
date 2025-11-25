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
 */
public record UpdateBookingStatusRequest(

    @NotBlank(message = "Target status is required")
    @Pattern(
        regexp = "^(RESERVED|FINISHED|CANCELED)$",
        message = "Target status must be RESERVED, FINISHED, or CANCELED"
    )
    String targetStatus,

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    String cancellationReason,

    @Size(max = 50, message = "Canceled by cannot exceed 50 characters")
    String canceledBy
) {

    /**
     * Validates that cancellation fields are provided when status is CANCELED.
     */
    public UpdateBookingStatusRequest {
        if ("CANCELED".equals(targetStatus)) {
            if (cancellationReason == null || cancellationReason.isBlank()) {
                throw new IllegalArgumentException("Cancellation reason is required when canceling a booking");
            }
            if (canceledBy == null || canceledBy.isBlank()) {
                throw new IllegalArgumentException("Canceled by is required when canceling a booking");
            }
        }
    }
}
