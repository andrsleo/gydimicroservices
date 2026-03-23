package com.affiliate.rentals.gydi.commissions.domain.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Event published when a booking is finished.
 * <p>
 * This event triggers commission creation for both host and affiliate.
 * Published from the bookings bounded context, consumed by commissions bounded
 * context.
 * </p>
 */
public record BookingFinishedEvent(
        Long bookingId,
        Long propertyId,
        Long hostId,
        Long affiliateId, // Nullable if booking not from referral
        BigDecimal totalAmount,
        String currency,
        BigDecimal hostCommissionRate, // Snapshot from reservation time
        BigDecimal affiliateCommissionRate, // Snapshot from reservation time (nullable)
        LocalDateTime finishedAt) {
    public BookingFinishedEvent {
        Objects.requireNonNull(bookingId, "Booking ID cannot be null");
        Objects.requireNonNull(propertyId, "Property ID cannot be null");
        Objects.requireNonNull(hostId, "Host ID cannot be null");
        Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(finishedAt, "Finished timestamp cannot be null");

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }

    /**
     * Checks if booking came from a real referral (has affiliate with positive commission rate).
     * <p>
     * Returns {@code false} for organic bookings where the affiliate is the system
     * organic user (system-organic@gydi.internal), identified by a zero affiliate
     * commission rate. Organic bookings generate no affiliate payout.
     * </p>
     */
    public boolean hasAffiliate() {
        return affiliateId != null
                && affiliateCommissionRate != null
                && affiliateCommissionRate.compareTo(BigDecimal.ZERO) > 0;
    }
}
