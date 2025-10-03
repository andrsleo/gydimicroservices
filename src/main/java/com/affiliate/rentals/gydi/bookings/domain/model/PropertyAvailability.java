package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PropertyAvailability value object representing a blocked date for a property.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It tracks property availability by representing dates when a property is not available
 * for booking. Each blocked date has an associated reason (booking, maintenance, owner use,
 * or manual block) and may be linked to a booking ID if the block is due to a reservation.</p>
 *
 * <p>This model is essential for preventing double bookings and managing property calendars.
 * The system creates availability records when bookings are confirmed and when owners manually
 * block dates for various reasons.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PropertyAvailability booked = PropertyAvailability.forBooking(1L, LocalDate.now(), 123L);
 * PropertyAvailability maintenance = PropertyAvailability.forMaintenance(1L, LocalDate.now().plusDays(5));
 * boolean isForBooking = booked.isForBooking();
 * boolean canOverride = maintenance.canBeOverridden();
 * }</pre>
 *
 * @param propertyId the ID of the property, never {@code null}
 * @param blockedDate the date that is blocked, never {@code null}
 * @param reason the reason for blocking, never {@code null}
 * @param bookingId the associated booking ID if reason is BOOKING, may be {@code null} otherwise
 * @param createdAt the timestamp when this availability record was created, never {@code null}
 * @author GYDI Development Team
 * @see BlockReason
 */
public record PropertyAvailability(
        Long propertyId,
        LocalDate blockedDate,
        BlockReason reason,
        Long bookingId,
        LocalDateTime createdAt
) {

    /**
     * Compact constructor that validates property availability data.
     *
     * @throws NullPointerException if propertyId, blockedDate, or reason is {@code null}
     */
    public PropertyAvailability {
        Objects.requireNonNull(propertyId, "Property ID cannot be null");
        Objects.requireNonNull(blockedDate, "Blocked date cannot be null");
        Objects.requireNonNull(reason, "Block reason cannot be null");
        createdAt = Objects.requireNonNullElseGet(createdAt, LocalDateTime::now);
    }

    /**
     * Factory method to create a PropertyAvailability record for a booking.
     * Associates the blocked date with a specific booking ID.
     *
     * @param propertyId the property ID
     * @param blockedDate the date to block
     * @param bookingId the booking ID causing the block
     * @return a new PropertyAvailability instance for a booking
     */
    public static PropertyAvailability forBooking(Long propertyId, LocalDate blockedDate, Long bookingId) {
        return new PropertyAvailability(propertyId, blockedDate, BlockReason.BOOKING, bookingId, LocalDateTime.now());
    }

    /**
     * Factory method to create a PropertyAvailability record for maintenance.
     * Marks a date as unavailable due to property maintenance or repairs.
     *
     * @param propertyId the property ID
     * @param blockedDate the date to block
     * @return a new PropertyAvailability instance for maintenance
     */
    public static PropertyAvailability forMaintenance(Long propertyId, LocalDate blockedDate) {
        return new PropertyAvailability(propertyId, blockedDate, BlockReason.MAINTENANCE, null, LocalDateTime.now());
    }

    /**
     * Factory method to create a PropertyAvailability record for owner use.
     * Marks a date as unavailable because the owner is using the property.
     *
     * @param propertyId the property ID
     * @param blockedDate the date to block
     * @return a new PropertyAvailability instance for owner use
     */
    public static PropertyAvailability forOwnerUse(Long propertyId, LocalDate blockedDate) {
        return new PropertyAvailability(propertyId, blockedDate, BlockReason.OWNER_USE, null, LocalDateTime.now());
    }

    /**
     * Factory method to create a PropertyAvailability record for a manual block.
     * Allows owners to manually block dates for any other reason.
     *
     * @param propertyId the property ID
     * @param blockedDate the date to block
     * @return a new PropertyAvailability instance for a manual block
     */
    public static PropertyAvailability blocked(Long propertyId, LocalDate blockedDate) {
        return new PropertyAvailability(propertyId, blockedDate, BlockReason.BLOCKED, null, LocalDateTime.now());
    }

    /**
     * Checks if this availability block is for an actual booking.
     * Returns true only if the reason is BOOKING and a booking ID is present.
     *
     * @return {@code true} if this is a booking block with a valid booking ID, {@code false} otherwise
     */
    public boolean isForBooking() {
        return reason.isBooking() && bookingId != null;
    }

    /**
     * Checks if this availability block can be overridden.
     * Delegates to the {@link BlockReason#canBeOverridden()} method.
     *
     * @return {@code true} if the block can be overridden, {@code false} otherwise
     */
    public boolean canBeOverridden() {
        return reason.canBeOverridden();
    }

    /**
     * Checks if this availability record is for a specific property.
     *
     * @param propertyId the property ID to check
     * @return {@code true} if this record belongs to the specified property, {@code false} otherwise
     */
    public boolean isForProperty(Long propertyId) {
        return this.propertyId.equals(propertyId);
    }

    /**
     * Checks if this availability record is for a specific date.
     *
     * @param date the date to check
     * @return {@code true} if the blocked date matches, {@code false} otherwise
     */
    public boolean isOn(LocalDate date) {
        return blockedDate.equals(date);
    }

    /**
     * Checks if the blocked date is before a specified date.
     *
     * @param date the date to compare against
     * @return {@code true} if the blocked date is before the specified date, {@code false} otherwise
     */
    public boolean isBefore(LocalDate date) {
        return blockedDate.isBefore(date);
    }

    /**
     * Checks if the blocked date is after a specified date.
     *
     * @param date the date to compare against
     * @return {@code true} if the blocked date is after the specified date, {@code false} otherwise
     */
    public boolean isAfter(LocalDate date) {
        return blockedDate.isAfter(date);
    }
}
