package com.affiliate.rentals.gydi.bookings.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain Event published when a new booking is created (REQUEST status).
 * <p>
 * <strong>Subscribers:</strong>
 * </p>
 * <ul>
 *   <li>NotificationService: Notifies property owner of new booking request</li>
 *   <li>ReferralService: Increments click/conversion counters (future)</li>
 * </ul>
 */
public record BookingCreatedEvent(
    Long bookingId,
    Long referralLinkId,
    Long propertyId,
    String clientEmail,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime occurredAt
) {
    public BookingCreatedEvent {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (referralLinkId == null) {
            throw new IllegalArgumentException("Referral link ID cannot be null");
        }
        if (propertyId == null) {
            throw new IllegalArgumentException("Property ID cannot be null");
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
