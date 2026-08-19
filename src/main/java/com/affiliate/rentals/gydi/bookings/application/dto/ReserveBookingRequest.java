package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command DTO for confirming a booking (REQUEST → RESERVED).
 * <p>
 * Used by ADMIN when confirming booking in Airbnb.
 * Optionally allows modifying check-in/check-out dates if they changed.
 * </p>
 */
public record ReserveBookingRequest(
    @NotBlank(message = "Airbnb confirmation code is required")
    String airbnbConfirmationCode,

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    BigDecimal totalAmount,

    @NotBlank(message = "Currency is required")
    String currency,

    @NotNull(message = "Admin ID is required")
    @Positive(message = "Admin ID must be positive")
    Long adminId,

    // Optional: allow modifying dates if they changed in Airbnb
    LocalDate checkInDate,

    LocalDate checkOutDate
) {
}
