package com.affiliate.rentals.gydi.bookings.domain.event;

import java.time.LocalDateTime;

/**
 * Domain Event published when a booking is canceled.
 * <p>
 * Cancellation details (reason, who canceled) are not tracked in the booking domain anymore.
 * If detailed cancellation tracking is needed, it can be handled in a separate audit system.
 * </p>
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
    LocalDateTime occurredAt
) {
    public BookingCanceledEvent {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
