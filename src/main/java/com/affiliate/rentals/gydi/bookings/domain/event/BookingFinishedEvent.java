package com.affiliate.rentals.gydi.bookings.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain Event published when a booking is finished (completed successfully).
 * <p>
 * This event triggers the creation of a payment.booking record.
 * </p>
 * <p>
 * <strong>Subscribers:</strong>
 * </p>
 * <ul>
 *   <li>PaymentEventHandler: Creates payment record with commission</li>
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
    BigDecimal totalAmount,
    String currency,
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
     * @param totalAmount the total booking amount
     * @param currency the currency (USD, EUR, etc.)
     * @param occurredAt when the event occurred
     */
    public BookingFinishedEvent {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
