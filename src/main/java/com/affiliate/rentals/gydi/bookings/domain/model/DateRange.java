package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * DateRange value object - represents a period between two dates.
 * Immutable by design following DDD principles.
 * Validates that end date is after start date.
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    public long numberOfNights() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) && !this.startDate.isAfter(other.endDate);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean isInFuture() {
        return startDate.isAfter(LocalDate.now());
    }

    public boolean isInPast() {
        return endDate.isBefore(LocalDate.now());
    }

    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    @Override
    public String toString() {
        return "%s to %s (%d nights)".formatted(startDate, endDate, numberOfNights());
    }
}