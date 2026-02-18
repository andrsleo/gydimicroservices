package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object representing a status change in booking history.
 * <p>
 * Immutable record of a state transition with audit information.
 * </p>
 */
public final class BookingStatusHistory {
    private final Long id;
    private final Long bookingId;
    private final BookingStatus oldStatus;
    private final BookingStatus newStatus;
    private final Long changedBy;
    private final String changeReason;
    private final boolean isAutomatic;
    private final LocalDateTime createdAt;

    private BookingStatusHistory(
        Long id,
        Long bookingId,
        BookingStatus oldStatus,
        BookingStatus newStatus,
        Long changedBy,
        String changeReason,
        boolean isAutomatic,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.bookingId = Objects.requireNonNull(bookingId, "Booking ID cannot be null");
        this.oldStatus = oldStatus; // Can be null for first status
        this.newStatus = Objects.requireNonNull(newStatus, "New status cannot be null");
        this.changedBy = changedBy; // Can be null for automatic transitions
        this.changeReason = changeReason;
        this.isAutomatic = isAutomatic;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates manual status change history.
     *
     * @param bookingId the booking ID
     * @param oldStatus previous status (null for initial)
     * @param newStatus new status
     * @param changedBy user who made the change (admin, host, etc.)
     * @param reason reason for the change
     * @return manual status history entry
     */
    public static BookingStatusHistory manual(
        Long bookingId,
        BookingStatus oldStatus,
        BookingStatus newStatus,
        Long changedBy,
        String reason
    ) {
        Objects.requireNonNull(changedBy, "Changed by user ID is required for manual transitions");
        return new BookingStatusHistory(
            null,
            bookingId,
            oldStatus,
            newStatus,
            changedBy,
            reason,
            false,
            LocalDateTime.now()
        );
    }

    /**
     * Factory method: Creates automatic status change history.
     * <p>
     * Used for scheduled transitions (RESERVED → IN_PROGRESS, IN_PROGRESS → FINISHED).
     * </p>
     *
     * @param bookingId the booking ID
     * @param oldStatus previous status
     * @param newStatus new status
     * @param reason reason for automatic transition
     * @return automatic status history entry
     */
    public static BookingStatusHistory automatic(
        Long bookingId,
        BookingStatus oldStatus,
        BookingStatus newStatus,
        String reason
    ) {
        return new BookingStatusHistory(
            null,
            bookingId,
            oldStatus,
            newStatus,
            null, // No user for automatic transitions
            reason,
            true,
            LocalDateTime.now()
        );
    }

    /**
     * Reconstitution constructor for persistence layer.
     */
    public static BookingStatusHistory reconstitute(
        Long id,
        Long bookingId,
        BookingStatus oldStatus,
        BookingStatus newStatus,
        Long changedBy,
        String changeReason,
        boolean isAutomatic,
        LocalDateTime createdAt
    ) {
        return new BookingStatusHistory(
            id,
            bookingId,
            oldStatus,
            newStatus,
            changedBy,
            changeReason,
            isAutomatic,
            createdAt
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public BookingStatus getOldStatus() {
        return oldStatus;
    }

    public BookingStatus getNewStatus() {
        return newStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingStatusHistory that = (BookingStatusHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "BookingStatusHistory{id=%d, booking=%d, %s → %s, auto=%b}",
            id, bookingId, oldStatus, newStatus, isAutomatic
        );
    }
}
