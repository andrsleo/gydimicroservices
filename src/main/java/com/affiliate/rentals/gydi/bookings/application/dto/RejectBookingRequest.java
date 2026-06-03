package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for HOST rejecting a booking request.
 */
public record RejectBookingRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
        String reason
) {}
