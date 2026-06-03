package com.affiliate.rentals.gydi.bookings.application.dto;

import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Booking read operations.
 */
public record BookingDto(
    Long id,
    Long referralLinkId,
    Long propertyId,

    // Dates
    LocalDate checkInDate,
    LocalDate checkOutDate,
    Integer numberOfNights,

    // Guest Info
    String guestName,
    String guestEmail,
    String guestPhone,
    Integer guestsCount,

    // Airbnb Integration
    String airbnbConfirmationCode,

    // Financial
    BigDecimal totalAmount,
    String currency,

    // Status
    BookingStatus status,
    List<BookingStatusHistoryDto> statusHistory,

    // Lifecycle Timestamps
    LocalDateTime reservedAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime cancelledAt,

    // Audit
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Fase 1 — Host direct confirmation
    String rejectionReason,

    // Fase 2 — Stripe payment fields
    String stripeBookingIntentId,
    String stripeDepositIntentId,
    BigDecimal depositAmount,
    String depositCurrency,
    LocalDateTime depositCapturedAt,
    BigDecimal depositCaptureAmount,
    LocalDateTime paymentReleasedAt
) {
}
