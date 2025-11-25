package com.affiliate.rentals.gydi.bookings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for booking information.
 * <p>
 * Used for all booking-related responses (create, update, query).
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
    BigDecimal totalAmount,
    String currency,
    String status,
    String createdAt
) {
}
