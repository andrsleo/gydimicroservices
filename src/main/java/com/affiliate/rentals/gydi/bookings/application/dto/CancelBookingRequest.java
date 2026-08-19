package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command DTO for cancelling a booking.
 * <p>
 * Used by ADMIN, host, or guest to cancel a booking.
 * </p>
 */
public record CancelBookingRequest(
    @NotNull(message = "Cancelled by user ID is required")
    @Positive(message = "User ID must be positive")
    Long cancelledBy,

    @NotBlank(message = "Cancellation reason is required")
    String reason
) {
}
