package com.affiliate.rentals.gydi.subscriptions.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Request DTO for calculating commission breakdown.
 *
 * <p>This DTO represents a request to calculate how commissions are distributed
 * for a booking transaction based on the subscription plans of the host and affiliate.</p>
 *
 * @param bookingAmount Total booking amount (must be >= 0)
 * @param hostPlan Host's subscription plan code (FREE, PRO, ELITE)
 * @param affiliatePlan Affiliate's subscription plan code (FREE, PRO, ELITE)
 *
 * @author GYDI Development Team
 * @version 2.0
 * @since 2026-02-04
 */
public record CalculateCommissionRequest(

        @NotNull(message = "Booking amount cannot be null")
        @DecimalMin(value = "0.00", message = "Booking amount must be zero or positive")
        BigDecimal bookingAmount,

        @NotBlank(message = "Host plan cannot be blank")
        @Pattern(regexp = "FREE|PRO|ELITE", message = "Host plan must be FREE, PRO, or ELITE")
        String hostPlan,

        @NotBlank(message = "Affiliate plan cannot be blank")
        @Pattern(regexp = "FREE|PRO|ELITE", message = "Affiliate plan must be FREE, PRO, or ELITE")
        String affiliatePlan

) {
    /**
     * Compact constructor for validation.
     * Ensures all fields meet business constraints.
     */
    public CalculateCommissionRequest {
        if (bookingAmount != null && bookingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Booking amount cannot be negative");
        }
    }
}
