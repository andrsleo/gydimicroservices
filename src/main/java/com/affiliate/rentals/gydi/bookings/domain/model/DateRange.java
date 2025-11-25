package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object representing a date range for a booking.
 * Immutable and self-validating.
 */
public final class DateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Creates a new DateRange.
     *
     * @param startDate the check-in date
     * @param endDate the check-out date
     * @throws IllegalArgumentException if dates are invalid
     */
    public DateRange(LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date must be in the present or future");
        }
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Business rule: Maximum booking duration is 365 days (1 year)
        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        if (nights > 365) {
            throw new IllegalArgumentException("Booking duration cannot exceed 365 days (1 year)");
        }

        // Business rule: Minimum booking duration is 1 night
        if (nights < 1) {
            throw new IllegalArgumentException("Booking must be at least 1 night");
        }
    }

    /**
     * Calculates the total number of nights in this date range.
     *
     * @return total nights (always >= 1)
     */
    public long totalNights() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Checks if this date range overlaps with another date range.
     *
     * @param other the other date range to check
     * @return true if the ranges overlap
     */
    public boolean overlaps(DateRange other) {
        // Two ranges overlap if: (StartA <= EndB) and (EndA >= StartB)
        return !this.endDate.isBefore(other.startDate)
            && !other.endDate.isBefore(this.startDate);
    }

    /**
     * Checks if a specific date falls within this date range (inclusive).
     *
     * @param date the date to check
     * @return true if date is within range
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if this date range is in the past.
     *
     * @return true if end date has passed
     */
    public boolean isPast() {
        return endDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if this date range is current (ongoing).
     *
     * @return true if today is within the range
     */
    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return contains(today);
    }

    /**
     * Checks if this date range is in the future.
     *
     * @return true if start date is after today
     */
    public boolean isFuture() {
        return startDate.isAfter(LocalDate.now());
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return Objects.equals(startDate, dateRange.startDate) &&
               Objects.equals(endDate, dateRange.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        return String.format("DateRange{%s to %s, %d nights}",
            startDate, endDate, totalNights());
    }
}
