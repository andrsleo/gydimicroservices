package com.affiliate.rentals.gydi.commissions.domain.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object representing affiliate commission payment schedule.
 * <p>
 * Payments are scheduled for the 1st or 15th of the month after the dispute period ends.
 * </p>
 */
public final class PaymentSchedule {
    private final LocalDate scheduledPaymentDate;

    private PaymentSchedule(LocalDate scheduledPaymentDate) {
        this.scheduledPaymentDate = Objects.requireNonNull(
            scheduledPaymentDate,
            "Scheduled payment date cannot be null"
        );
    }

    /**
     * Calculates payment date based on when dispute period ends.
     * <p>
     * Rules:
     * - If dispute ends on day 1-14: Schedule for 15th of same month
     * - If dispute ends on day 15+: Schedule for 1st of next month
     * </p>
     */
    public static PaymentSchedule calculate(LocalDateTime disputePeriodEndsAt) {
        Objects.requireNonNull(disputePeriodEndsAt, "Dispute period end date cannot be null");

        LocalDate baseDate = disputePeriodEndsAt.toLocalDate();
        int dayOfMonth = baseDate.getDayOfMonth();

        LocalDate paymentDate;
        if (dayOfMonth < 15) {
            // Schedule for 15th of current month
            paymentDate = LocalDate.of(baseDate.getYear(), baseDate.getMonth(), 15);
        } else {
            // Schedule for 1st of next month
            paymentDate = baseDate.plusMonths(1).withDayOfMonth(1);
        }

        return new PaymentSchedule(paymentDate);
    }

    /**
     * Reconstitutes PaymentSchedule from database value.
     */
    public static PaymentSchedule reconstitute(LocalDate scheduledPaymentDate) {
        return new PaymentSchedule(scheduledPaymentDate);
    }

    /**
     * Checks if payment is due (scheduled date is today or past).
     */
    public boolean isDue() {
        return !scheduledPaymentDate.isAfter(LocalDate.now());
    }

    /**
     * Checks if payment is scheduled for today.
     */
    public boolean isDueToday() {
        return scheduledPaymentDate.equals(LocalDate.now());
    }

    public LocalDate getScheduledPaymentDate() {
        return scheduledPaymentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentSchedule that = (PaymentSchedule) o;
        return Objects.equals(scheduledPaymentDate, that.scheduledPaymentDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduledPaymentDate);
    }

    @Override
    public String toString() {
        return "PaymentSchedule{scheduledFor=" + scheduledPaymentDate + "}";
    }
}
