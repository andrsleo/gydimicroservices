package com.affiliate.rentals.gydi.bookings.application.dto;

import java.math.BigDecimal;

/**
 * DTO for Booking statistics.
 * <p>
 * Used for dashboard stats for affiliates and hosts.
 * </p>
 */
public record BookingStatsDto(
    Integer totalBookings,
    Integer activeBookings,
    BigDecimal totalRevenue,
    String currency
) {
    public BookingStatsDto {
        // Set default currency if null
        currency = currency != null ? currency : "USD";
    }
}
