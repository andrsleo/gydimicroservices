package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for HOST capturing security deposit due to damage (Phase 2).
 */
public record CaptureDamageDepositRequest(
        /**
         * Amount to capture in cents. If null, captures the full authorized deposit.
         */
        @Positive(message = "Damage amount must be positive if provided")
        Long damageAmountCents,

        @NotBlank(message = "Damage description is required")
        @Size(max = 500, message = "Damage description cannot exceed 500 characters")
        String description
) {}
