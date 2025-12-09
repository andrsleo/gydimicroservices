package com.affiliate.rentals.gydi.bookings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for booking information.
 * <p>
 * Used for all booking-related responses (create, update, query).
 * Financial details are not included here - they are tracked in commission_booking table.
 * </p>
 */
public record BookingResponse(
    Long id,
    Long referralLinkId,
    Long propertyId,
    LocalDate startDate,
    LocalDate endDate,
    String clientEmail,
    String clientFullName,
    String status,
    String createdAt
) {
}
