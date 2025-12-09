package com.affiliate.rentals.gydi.bookings.domain.model;

import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Booking Aggregate Root.
 * <p>
 * Represents a booking request for a property through a referral link.
 * Manages the complete lifecycle: REQUEST → RESERVED → FINISHED or CANCELED.
 * <p>
 * <strong>PURE DOMAIN MODEL</strong> - No framework dependencies (no JPA, Spring, etc.)
 * </p>
 */
public class Booking {
    // Identity
    private Long id;

    // Associations (IDs only, following DDD)
    private final Long referralLinkId;
    private final Long propertyId;

    // Value Objects
    private final DateRange dateRange;
    private final ClientInfo clientInfo;

    // State
    private BookingStatus status;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - use factory methods.
     */
    private Booking(Long id,
                    Long referralLinkId,
                    Long propertyId,
                    DateRange dateRange,
                    ClientInfo clientInfo,
                    BookingStatus status,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.referralLinkId = Objects.requireNonNull(referralLinkId, "Referral link ID cannot be null");
        this.propertyId = Objects.requireNonNull(propertyId, "Property ID cannot be null");
        this.dateRange = Objects.requireNonNull(dateRange, "Date range cannot be null");
        this.clientInfo = Objects.requireNonNull(clientInfo, "Client info cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates a new booking in REQUEST status.
     * <p>
     * This is the entry point when a client submits a quotation request.
     * </p>
     *
     * @param referralLinkId the referral link used
     * @param propertyId the property being booked
     * @param dateRange the requested dates
     * @param clientInfo the client's information
     * @return a new Booking in REQUEST status
     */
    public static Booking createRequest(Long referralLinkId,
                                         Long propertyId,
                                         DateRange dateRange,
                                         ClientInfo clientInfo) {
        return new Booking(
            null, // ID assigned by persistence layer
            referralLinkId,
            propertyId,
            dateRange,
            clientInfo,
            BookingStatus.REQUEST, // Initial status
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /**
     * Reconstitution constructor for persistence layer.
     * Used when loading existing booking from database.
     */
    public static Booking reconstitute(Long id,
                                        Long referralLinkId,
                                        Long propertyId,
                                        DateRange dateRange,
                                        ClientInfo clientInfo,
                                        BookingStatus status,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new Booking(
            id,
            referralLinkId,
            propertyId,
            dateRange,
            clientInfo,
            status,
            createdAt,
            updatedAt
        );
    }

    // ========== BUSINESS LOGIC: STATE TRANSITIONS ==========

    /**
     * Transitions booking from REQUEST to RESERVED.
     * <p>
     * Called when property owner accepts the booking request.
     * </p>
     *
     * @throws InvalidBookingStateException if current status is not REQUEST
     */
    public void reserve() {
        if (!status.canTransitionTo(BookingStatus.RESERVED)) {
            throw new InvalidBookingStateException(status, BookingStatus.RESERVED);
        }
        this.status = BookingStatus.RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Transitions booking from RESERVED to FINISHED.
     * <p>
     * Called when client checks out and stay is completed.
     * <strong>TRIGGER:</strong> This should publish a BookingFinishedEvent
     * which will create a payment.booking record.
     * </p>
     *
     * @throws InvalidBookingStateException if current status is not RESERVED
     */
    public void finish() {
        if (!status.canTransitionTo(BookingStatus.FINISHED)) {
            throw new InvalidBookingStateException(status, BookingStatus.FINISHED);
        }
        this.status = BookingStatus.FINISHED;
        this.updatedAt = LocalDateTime.now();

        // NOTE: Domain event publishing is handled by the use case layer
        // The use case will publish BookingFinishedEvent after persisting this change
    }

    /**
     * Cancels the booking.
     * <p>
     * Can be called from REQUEST or RESERVED status.
     * Cannot cancel FINISHED bookings.
     * </p>
     *
     * @throws InvalidBookingStateException if booking is FINISHED or already CANCELED
     */
    public void cancel() {
        if (!status.canTransitionTo(BookingStatus.CANCELED)) {
            throw new InvalidBookingStateException(
                String.format("Cannot cancel booking in %s status", status)
            );
        }

        this.status = BookingStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== QUERY METHODS ==========

    /**
     * Checks if booking can still be modified (not in terminal state).
     */
    public boolean isModifiable() {
        return status.isActive();
    }

    /**
     * Checks if booking is completed successfully.
     */
    public boolean isCompleted() {
        return status == BookingStatus.FINISHED;
    }

    /**
     * Checks if booking was canceled.
     */
    public boolean isCanceled() {
        return status == BookingStatus.CANCELED;
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        // Only used by persistence layer when assigning ID
        if (this.id != null) {
            throw new IllegalStateException("Cannot change booking ID once set");
        }
        this.id = id;
    }

    public Long getReferralLinkId() {
        return referralLinkId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "Booking{id=%d, property=%d, dates=%s, status=%s, client=%s}",
            id, propertyId, dateRange, status, clientInfo
        );
    }
}
