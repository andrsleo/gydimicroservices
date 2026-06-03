package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Command DTO for creating a new booking request.
 * <p>
 * Used when a guest submits a booking request via referral link.
 * </p>
 */
public record CreateBookingRequest(
    @NotNull(message = "Referral link ID is required")
    @Positive(message = "Referral link ID must be positive")
    Long referralLinkId,

    @NotNull(message = "Property ID is required")
    @Positive(message = "Property ID must be positive")
    Long propertyId,

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    LocalDate checkInDate,

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    LocalDate checkOutDate,

    @NotBlank(message = "Guest name is required")
    @Size(max = 255, message = "Guest name cannot exceed 255 characters")
    String guestName,

    @NotBlank(message = "Guest email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String guestEmail,

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    String guestPhone,

    @NotNull(message = "Guests count is required")
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 50, message = "Maximum 50 guests allowed")
    Integer guestsCount,

    // Phase 4 — Social Commerce: optional reference to content post that led to this booking
    @Positive(message = "Content post ID must be positive")
    Long contentPostId
) {
    /**
     * Validates that check-out is after check-in.
     * Called automatically by record compact constructor.
     */
    public CreateBookingRequest {
        if (checkOutDate != null && checkInDate != null && !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }
}
