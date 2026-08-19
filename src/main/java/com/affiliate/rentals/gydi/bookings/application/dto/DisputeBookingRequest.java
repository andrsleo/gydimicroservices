package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command DTO for disputing a completed booking.
 * <p>
 * Used to mark FINISHED bookings as DISPUTED when there's an issue.
 * </p>
 */
public record DisputeBookingRequest(
    @NotNull(message = "Disputed by user ID is required")
    @Positive(message = "User ID must be positive")
    Long disputedBy,

    @NotBlank(message = "Dispute reason is required")
    String reason
) {
}
