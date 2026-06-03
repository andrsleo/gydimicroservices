package com.affiliate.rentals.gydi.calendar.domain.model;

import com.affiliate.rentals.gydi.properties.domain.model.Money;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Aggregate root representing one day in a property's calendar.
 *
 * <p>Immutable domain model — all state transitions return new instances.
 * No Spring, JPA, or Lombok dependencies.</p>
 */
public final class CalendarDay {

    private final Long id;
    private final Long propertyId;
    private final LocalDate calendarDate;
    private final CalendarDayStatus status;
    private final Money customPrice;
    private final BlockSource blockSource;
    private final Long bookingId;
    private final String notes;
    private final Long createdBy;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;

    private CalendarDay(
            Long id,
            Long propertyId,
            LocalDate calendarDate,
            CalendarDayStatus status,
            Money customPrice,
            BlockSource blockSource,
            Long bookingId,
            String notes,
            Long createdBy,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        this.id = id;
        this.propertyId = Objects.requireNonNull(propertyId, "propertyId cannot be null");
        this.calendarDate = Objects.requireNonNull(calendarDate, "calendarDate cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.customPrice = customPrice;
        this.blockSource = blockSource;
        this.bookingId = bookingId;
        this.notes = notes;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a manual block for a calendar day.
     * Date range validation (future-only) is enforced by the use case, not here.
     */
    public static CalendarDay blockManual(Long propertyId, LocalDate date, Long hostId) {
        ZonedDateTime now = ZonedDateTime.now();
        return new CalendarDay(
                null,
                propertyId,
                date,
                CalendarDayStatus.BLOCKED,
                null,
                BlockSource.MANUAL,
                null,
                null,
                hostId,
                now,
                now);
    }

    /**
     * Creates a reservation block linked to a specific booking.
     */
    public static CalendarDay reserveForBooking(Long propertyId, LocalDate date, Long bookingId, Long hostId) {
        ZonedDateTime now = ZonedDateTime.now();
        return new CalendarDay(
                null,
                propertyId,
                date,
                CalendarDayStatus.RESERVED,
                null,
                BlockSource.BOOKING,
                Objects.requireNonNull(bookingId, "bookingId cannot be null"),
                null,
                hostId,
                now,
                now);
    }

    /**
     * Creates a maintenance block for a calendar day.
     */
    public static CalendarDay blockForMaintenance(Long propertyId, LocalDate date, Long hostId) {
        ZonedDateTime now = ZonedDateTime.now();
        return new CalendarDay(
                null,
                propertyId,
                date,
                CalendarDayStatus.MAINTENANCE,
                null,
                BlockSource.MAINTENANCE,
                null,
                null,
                hostId,
                now,
                now);
    }

    /**
     * Reconstructs a CalendarDay from persistence — all fields including id.
     */
    public static CalendarDay reconstruct(
            Long id,
            Long propertyId,
            LocalDate date,
            CalendarDayStatus status,
            Money customPrice,
            BlockSource blockSource,
            Long bookingId,
            String notes,
            Long createdBy,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
        return new CalendarDay(
                id, propertyId, date, status, customPrice, blockSource,
                bookingId, notes, createdBy, createdAt, updatedAt);
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /** Returns true if this day is available for booking. */
    public boolean isAvailable() {
        return status == CalendarDayStatus.AVAILABLE;
    }

    /** Returns true if a custom price has been set for this day. */
    public boolean hasCustomPrice() {
        return customPrice != null;
    }

    /** Returns a new CalendarDay with the given custom price applied. */
    public CalendarDay withCustomPrice(Money price) {
        Objects.requireNonNull(price, "price cannot be null");
        return new CalendarDay(
                this.id, this.propertyId, this.calendarDate, this.status,
                price, this.blockSource, this.bookingId, this.notes,
                this.createdBy, this.createdAt, ZonedDateTime.now());
    }

    /** Returns a new CalendarDay with the custom price removed. */
    public CalendarDay removeCustomPrice() {
        return new CalendarDay(
                this.id, this.propertyId, this.calendarDate, this.status,
                null, this.blockSource, this.bookingId, this.notes,
                this.createdBy, this.createdAt, ZonedDateTime.now());
    }

    /**
     * Returns a new CalendarDay with status AVAILABLE, blockSource and bookingId cleared.
     * Only allowed when status is BLOCKED or MAINTENANCE. RESERVED days cannot be unblocked
     * via this method — use {@code CannotUnblockReservedDateException} instead.
     *
     * @throws com.affiliate.rentals.gydi.calendar.domain.exception.CannotUnblockReservedDateException
     *         if this day is RESERVED
     */
    public CalendarDay unblock() {
        if (this.status == CalendarDayStatus.RESERVED) {
            throw new com.affiliate.rentals.gydi.calendar.domain.exception.CannotUnblockReservedDateException(
                    this.calendarDate, this.propertyId);
        }
        return new CalendarDay(
                this.id, this.propertyId, this.calendarDate, CalendarDayStatus.AVAILABLE,
                this.customPrice, null, null, this.notes,
                this.createdBy, this.createdAt, ZonedDateTime.now());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getPropertyId() { return propertyId; }
    public LocalDate getCalendarDate() { return calendarDate; }
    public CalendarDayStatus getStatus() { return status; }
    public Money getCustomPrice() { return customPrice; }
    public BlockSource getBlockSource() { return blockSource; }
    public Long getBookingId() { return bookingId; }
    public String getNotes() { return notes; }
    public Long getCreatedBy() { return createdBy; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalendarDay other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(propertyId, other.propertyId)
                && Objects.equals(calendarDate, other.calendarDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, propertyId, calendarDate);
    }

    @Override
    public String toString() {
        return "CalendarDay{id=%d, propertyId=%d, date=%s, status=%s}"
                .formatted(id, propertyId, calendarDate, status);
    }
}
