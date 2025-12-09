package com.affiliate.rentals.gydi.bookings.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain Event published when a booking is finished (completed successfully).
 * <p>
 * This event triggers the creation of a commission.booking record.
 * Financial details (amount, currency) are not part of the booking domain anymore.
 * </p>
 * <p>
 * <strong>Subscribers:</strong>
 * </p>
 * <ul>
 *   <li>PaymentEventHandler: Creates commission record (future)</li>
 *   <li>NotificationService: Sends confirmation emails (future)</li>
 *   <li>AnalyticsService: Records conversion metric (future)</li>
 * </ul>
 */
public record BookingFinishedEvent(
    Long bookingId,
    Long referralLinkId,
    Long propertyId,
    Long userId, // The referring user who will receive commission
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime occurredAt
) {
    /**
     * Creates a BookingFinishedEvent.
     *
     * @param bookingId the booking that was finished
     * @param referralLinkId the referral link used
     * @param propertyId the property that was booked
     * @param userId the user who created the referral link (earns commission)
     * @param startDate check-in date
     * @param endDate check-out date
     * @param occurredAt when the event occurred
     */
    public BookingFinishedEvent {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
