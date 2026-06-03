package com.affiliate.rentals.gydi.bookings.domain.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Domain model for property availability calendar.
 * <p>
 * Represents a single blocked date for a property.
 * Immutable — use factory methods to create instances.
 * </p>
 */
public class PropertyCalendar {

    public enum BlockReason { MANUAL, BOOKING, MAINTENANCE }

    private final Long id;
    private final Long propertyId;
    private final LocalDate blockedDate;
    private final BlockReason blockReason;
    private final Long bookingId;   // null si no fue bloqueado por reserva
    private final Long createdByHostId;
    private final ZonedDateTime createdAt;

    // Constructor completo (reconstrucción desde DB)
    public PropertyCalendar(Long id, Long propertyId, LocalDate blockedDate,
                            BlockReason blockReason, Long bookingId,
                            Long createdByHostId, ZonedDateTime createdAt) {
        this.id = id;
        this.propertyId = propertyId;
        this.blockedDate = blockedDate;
        this.blockReason = blockReason;
        this.bookingId = bookingId;
        this.createdByHostId = createdByHostId;
        this.createdAt = createdAt;
    }

    /**
     * Factory method para bloqueo manual del host.
     */
    public static PropertyCalendar blockManual(Long propertyId, LocalDate date, Long hostId) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot block past dates");
        }
        return new PropertyCalendar(null, propertyId, date, BlockReason.MANUAL,
                                    null, hostId, ZonedDateTime.now());
    }

    /**
     * Factory method para bloqueo automático por reserva confirmada.
     * Bloquea todas las fechas desde checkIn hasta checkOut (inclusive).
     */
    public static List<PropertyCalendar> blockForBooking(Long propertyId,
                                                          LocalDate checkIn,
                                                          LocalDate checkOut,
                                                          Long bookingId,
                                                          Long hostId) {
        List<PropertyCalendar> blocked = new ArrayList<>();
        LocalDate current = checkIn;
        while (!current.isAfter(checkOut)) {
            blocked.add(new PropertyCalendar(null, propertyId, current,
                    BlockReason.BOOKING, bookingId, hostId, ZonedDateTime.now()));
            current = current.plusDays(1);
        }
        return blocked;
    }

    public boolean isBlockedByBooking() {
        return blockReason == BlockReason.BOOKING && bookingId != null;
    }

    // Getters (sin setters — inmutable)
    public Long getId() { return id; }
    public Long getPropertyId() { return propertyId; }
    public LocalDate getBlockedDate() { return blockedDate; }
    public BlockReason getBlockReason() { return blockReason; }
    public Long getBookingId() { return bookingId; }
    public Long getCreatedByHostId() { return createdByHostId; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
}
