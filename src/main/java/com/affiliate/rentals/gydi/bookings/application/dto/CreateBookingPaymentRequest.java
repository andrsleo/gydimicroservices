package com.affiliate.rentals.gydi.bookings.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating booking payment (Phase 2).
 * Guest provides Stripe PaymentMethod ID.
 */
public record CreateBookingPaymentRequest(
        @NotBlank(message = "Stripe payment method ID is required")
        String stripePaymentMethodId
) {}
