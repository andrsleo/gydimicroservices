package com.affiliate.rentals.gydi.commissions.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object representing the 7-day dispute protection period.
 * <p>
 * Affiliate commissions cannot be paid until the dispute period ends.
 * This protects the platform from paying commissions for bookings that may be disputed.
 * </p>
 */
public final class DisputePeriod {
    private static final int DISPUTE_PERIOD_DAYS = 7;

    private final LocalDateTime disputePeriodEndsAt;

    private DisputePeriod(LocalDateTime disputePeriodEndsAt) {
        this.disputePeriodEndsAt = Objects.requireNonNull(
            disputePeriodEndsAt,
            "Dispute period end date cannot be null"
        );
    }

    /**
     * Calculates dispute period based on booking finish date.
     * <p>
     * Dispute period = finishedAt + 7 days
     * </p>
     */
    public static DisputePeriod calculate(LocalDateTime bookingFinishedAt) {
        Objects.requireNonNull(bookingFinishedAt, "Booking finished date cannot be null");

        LocalDateTime disputeEndsAt = bookingFinishedAt.plusDays(DISPUTE_PERIOD_DAYS);
        return new DisputePeriod(disputeEndsAt);
    }

    /**
     * Reconstitutes DisputePeriod from database value.
     */
    public static DisputePeriod reconstitute(LocalDateTime disputePeriodEndsAt) {
        return new DisputePeriod(disputePeriodEndsAt);
    }

    /**
     * Checks if dispute period is still active (commission cannot be approved yet).
     */
    public boolean isActive() {
        return LocalDateTime.now().isBefore(disputePeriodEndsAt);
    }

    /**
     * Checks if dispute period has ended (commission can be approved for payment).
     */
    public boolean hasEnded() {
        return !isActive();
    }

    public LocalDateTime getDisputePeriodEndsAt() {
        return disputePeriodEndsAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisputePeriod that = (DisputePeriod) o;
        return Objects.equals(disputePeriodEndsAt, that.disputePeriodEndsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disputePeriodEndsAt);
    }

    @Override
    public String toString() {
        return "DisputePeriod{endsAt=" + disputePeriodEndsAt +
               ", isActive=" + isActive() + "}";
    }
}
