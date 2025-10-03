package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * DateRange value object representing a period between two dates.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It represents a date range with a start and end date, commonly used for booking periods.
 * The class ensures that the end date is always after the start date and provides utility
 * methods for date range operations such as overlap detection and duration calculation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DateRange range = DateRange.of(
 *     LocalDate.of(2024, 6, 1),
 *     LocalDate.of(2024, 6, 7)
 * );
 * long nights = range.numberOfNights(); // 6
 * boolean isNow = range.isCurrent();
 * }</pre>
 *
 * @param startDate the start date of the range, never {@code null}
 * @param endDate the end date of the range, never {@code null} and must be after startDate
 * @author GYDI Development Team
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

    /**
     * Compact constructor that validates the date range.
     * Ensures end date is after start date.
     *
     * @throws NullPointerException if startDate or endDate is {@code null}
     * @throws IllegalArgumentException if endDate is not after startDate
     */
    public DateRange {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    /**
     * Factory method to create a DateRange from start and end dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return a new DateRange instance
     * @throws NullPointerException if startDate or endDate is {@code null}
     * @throws IllegalArgumentException if endDate is not after startDate
     */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    /**
     * Calculates the number of nights in this date range.
     * For accommodation bookings, this represents the number of nights stayed.
     *
     * @return the number of nights (days between start and end date)
     */
    public long numberOfNights() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Checks if this date range overlaps with another date range.
     * Two ranges overlap if they have any dates in common.
     *
     * @param other the other date range to check against
     * @return {@code true} if the ranges overlap, {@code false} otherwise
     */
    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) && !this.startDate.isAfter(other.endDate);
    }

    /**
     * Checks if a specific date falls within this date range (inclusive).
     *
     * @param date the date to check
     * @return {@code true} if the date is within the range, {@code false} otherwise
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if this date range is entirely in the future.
     * The range is considered future if the start date is after today.
     *
     * @return {@code true} if the range is in the future, {@code false} otherwise
     */
    public boolean isInFuture() {
        return startDate.isAfter(LocalDate.now());
    }

    /**
     * Checks if this date range is entirely in the past.
     * The range is considered past if the end date is before today.
     *
     * @return {@code true} if the range is in the past, {@code false} otherwise
     */
    public boolean isInPast() {
        return endDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if this date range includes today's date.
     * The range is considered current if today falls within the range (inclusive).
     *
     * @return {@code true} if today is within the range, {@code false} otherwise
     */
    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    @Override
    public String toString() {
        return "%s to %s (%d nights)".formatted(startDate, endDate, numberOfNights());
    }
}
