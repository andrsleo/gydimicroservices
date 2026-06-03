package com.affiliate.rentals.gydi.content.application.dto;

/**
 * Social commerce stats for a content post:
 * how many bookings it generated and the total revenue attributed.
 */
public record ContentBookingStatsDto(
    Long contentPostId,
    Integer bookingCount,
    Double totalRevenue,
    Double conversionRate   // bookings / views (0.0 to 1.0)
) {}
