package com.affiliate.rentals.gydi.bookings.domain.event;

import java.time.LocalDateTime;

/**
 * Domain Event published when a booking is canceled.
 * <p>
 * <strong>Subscribers:</strong>
 * </p>
 * <ul>
 *   <li>NotificationService: Notifies client and owner of cancellation</li>
 *   <li>PaymentService: Cancels pending payment if exists (future)</li>
 * </ul>
 */
public record BookingCanceledEvent(
    Long bookingId,
    Long propertyId,
    String canceledBy, // CLIENT, OWNER, ADMIN, SYSTEM
    String reason,
    LocalDateTime occurredAt
) {
    public BookingCanceledEvent {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (canceledBy == null || canceledBy.isBlank()) {
            throw new IllegalArgumentException("Canceled by cannot be null or empty");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
