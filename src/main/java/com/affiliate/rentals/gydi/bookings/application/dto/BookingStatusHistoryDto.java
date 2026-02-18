package com.affiliate.rentals.gydi.bookings.application.dto;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;

import java.time.LocalDateTime;

/**
 * DTO for BookingStatusHistory read operations.
 */
public record BookingStatusHistoryDto(
    Long id,
    Long bookingId,
    BookingStatus oldStatus,
    BookingStatus newStatus,
    Long changedBy,
    String changeReason,
    Boolean isAutomatic,
    LocalDateTime createdAt
) {
}
