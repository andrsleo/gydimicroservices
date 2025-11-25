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

    // Financial
    private final BigDecimal totalAmount;
    private final String currency;

    // State
    private BookingStatus status;

    // Cancellation details (only populated when CANCELED)
    private String cancellationReason;
    private String canceledBy; // CLIENT, OWNER, ADMIN, SYSTEM
    private LocalDateTime canceledAt;

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
                    BigDecimal totalAmount,
                    String currency,
                    BookingStatus status,
                    String cancellationReason,
                    String canceledBy,
                    LocalDateTime canceledAt,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.referralLinkId = Objects.requireNonNull(referralLinkId, "Referral link ID cannot be null");
        this.propertyId = Objects.requireNonNull(propertyId, "Property ID cannot be null");
        this.dateRange = Objects.requireNonNull(dateRange, "Date range cannot be null");
        this.clientInfo = Objects.requireNonNull(clientInfo, "Client info cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.cancellationReason = cancellationReason;
        this.canceledBy = canceledBy;
        this.canceledAt = canceledAt;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());

        validateBusinessRules();
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
     * @param totalAmount the total booking amount
     * @param currency the currency (USD, EUR, etc.)
     * @return a new Booking in REQUEST status
     */
    public static Booking createRequest(Long referralLinkId,
                                         Long propertyId,
                                         DateRange dateRange,
                                         ClientInfo clientInfo,
                                         BigDecimal totalAmount,
                                         String currency) {
        return new Booking(
            null, // ID assigned by persistence layer
            referralLinkId,
            propertyId,
            dateRange,
            clientInfo,
            totalAmount,
            currency,
            BookingStatus.REQUEST, // Initial status
            null,
            null,
            null,
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
                                        BigDecimal totalAmount,
                                        String currency,
                                        BookingStatus status,
                                        String cancellationReason,
                                        String canceledBy,
                                        LocalDateTime canceledAt,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new Booking(
            id,
            referralLinkId,
            propertyId,
            dateRange,
            clientInfo,
            totalAmount,
            currency,
            status,
            cancellationReason,
            canceledBy,
            canceledAt,
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
     * @param reason the cancellation reason (required)
     * @param canceledBy who initiated the cancellation (CLIENT, OWNER, ADMIN, SYSTEM)
     * @throws InvalidBookingStateException if booking is FINISHED or already CANCELED
     * @throws IllegalArgumentException if reason or canceledBy is null/blank
     */
    public void cancel(String reason, String canceledBy) {
        if (!status.canTransitionTo(BookingStatus.CANCELED)) {
            throw new InvalidBookingStateException(
                String.format("Cannot cancel booking in %s status", status)
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        if (canceledBy == null || canceledBy.isBlank()) {
            throw new IllegalArgumentException("Canceled by is required");
        }

        // Validate canceledBy values
        if (!canceledBy.matches("CLIENT|OWNER|ADMIN|SYSTEM")) {
            throw new IllegalArgumentException(
                "Canceled by must be one of: CLIENT, OWNER, ADMIN, SYSTEM"
            );
        }

        this.status = BookingStatus.CANCELED;
        this.cancellationReason = reason.trim();
        this.canceledBy = canceledBy.trim();
        this.canceledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== BUSINESS RULES VALIDATION ==========

    private void validateBusinessRules() {
        // Validate total amount
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        // Validate currency
        if (!currency.matches("USD|EUR|GBP|MXN|COP")) {
            throw new IllegalArgumentException(
                "Invalid currency. Allowed: USD, EUR, GBP, MXN, COP"
            );
        }

        // If status is CANCELED, must have cancellation details
        if (status == BookingStatus.CANCELED) {
            if (cancellationReason == null || cancellationReason.isBlank()) {
                throw new IllegalArgumentException(
                    "Canceled bookings must have a cancellation reason"
                );
            }
            if (canceledAt == null) {
                throw new IllegalArgumentException(
                    "Canceled bookings must have a cancellation timestamp"
                );
            }
        }
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

    /**
     * Calculates commission amount for this booking.
     * <p>
     * Used when creating payment.booking record.
     * Commission rate comes from user's subscription plan.
     * </p>
     *
     * @param commissionPercentage the commission rate (e.g., 5.00 for 5%)
     * @return the commission amount
     */
    public BigDecimal calculateCommission(BigDecimal commissionPercentage) {
        if (commissionPercentage == null || commissionPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Commission percentage must be >= 0");
        }
        if (commissionPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Commission percentage cannot exceed 100");
        }

        return totalAmount
            .multiply(commissionPercentage)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getCanceledBy() {
        return canceledBy;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
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
