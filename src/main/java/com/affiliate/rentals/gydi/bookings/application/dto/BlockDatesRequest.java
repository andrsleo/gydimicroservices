package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for HOST blocking dates on a property calendar.
 */
public record BlockDatesRequest(
        @NotEmpty(message = "At least one date must be provided")
        List<LocalDate> dates
) {}
