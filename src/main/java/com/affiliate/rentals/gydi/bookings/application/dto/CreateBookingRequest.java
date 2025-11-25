package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new booking.
 * <p>
 * Used when a client submits a booking request via referral link.
 * </p>
 */
public record CreateBookingRequest(

    @NotNull(message = "Referral link ID is required")
    @Positive(message = "Referral link ID must be positive")
    Long referralLinkId,

    @NotNull(message = "Property ID is required")
    @Positive(message = "Property ID must be positive")
    Long propertyId,

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    LocalDate endDate,

    @NotBlank(message = "Client email is required")
    @Email(message = "Invalid email format")
    String clientEmail,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String clientPhone,

    @NotBlank(message = "Client first name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    String clientFirstName,

    @NotBlank(message = "Client last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    String clientLastName,

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    BigDecimal totalAmount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO 4217)")
    String currency
) {

    /**
     * Validates that endDate is after startDate.
     */
    public CreateBookingRequest {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
