package com.affiliate.rentals.gydi.bookings.domain.model;

import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.BookingDates;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.GuestInfo;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Booking Aggregate Root.
 * <p>
 * Represents a booking request for a property through a referral link.
 * Manages the complete lifecycle: REQUEST → RESERVED → IN_PROGRESS → FINISHED.
 * </p>
 * <p>
 * <strong>PURE DOMAIN MODEL</strong> - No framework dependencies (no JPA,
 * Spring, etc.)
 * </p>
 */
public class Booking {
    // Identity
    private Long id;

    // Associations (IDs only, following DDD)
    private final Long referralLinkId;
    private final Long propertyId;

    // Value Objects
    private BookingDates bookingDates; // Mutable to allow date updates when confirming in Airbnb
    private final GuestInfo guestInfo;

    // Airbnb Integration
    private String airbnbConfirmationCode;

    // Financial (nullable in REQUEST, required in RESERVED)
    private Money totalAmount;
    private BigDecimal hostCommissionRate; // Snapshot at reservation
    private BigDecimal affiliateCommissionRate; // Snapshot at reservation

    // Stripe Payment — Phase 2 (nullable until payment is initiated)
    private String stripeBookingIntentId;
    private String stripeDepositIntentId;
    private Money depositAmount;
    private LocalDateTime depositCapturedAt;
    private BigDecimal depositCaptureAmount;
    private LocalDateTime paymentReleasedAt;

    // Fase 1 — Rejection reason (set when host rejects a booking request)
    private String rejectionReason;

    // Phase 4 — Social Commerce: content post that originated this booking (nullable)
    private Long contentPostId;

    // State
    private BookingStatus status;

    // Status History (managed by aggregate)
    private final List<BookingStatusHistory> statusHistory;

    // Lifecycle Timestamps
    private LocalDateTime reservedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - use factory methods.
     */
    private Booking(
            Long id,
            Long referralLinkId,
            Long propertyId,
            BookingDates bookingDates,
            GuestInfo guestInfo,
            String airbnbConfirmationCode,
            Money totalAmount,
            BigDecimal hostCommissionRate,
            BigDecimal affiliateCommissionRate,
            BookingStatus status,
            List<BookingStatusHistory> statusHistory,
            LocalDateTime reservedAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.referralLinkId = Objects.requireNonNull(referralLinkId, "Referral link ID cannot be null");
        this.propertyId = Objects.requireNonNull(propertyId, "Property ID cannot be null");
        this.bookingDates = Objects.requireNonNull(bookingDates, "Booking dates cannot be null");
        this.guestInfo = Objects.requireNonNull(guestInfo, "Guest info cannot be null");
        this.airbnbConfirmationCode = airbnbConfirmationCode;
        this.totalAmount = totalAmount;
        this.hostCommissionRate = hostCommissionRate;
        this.affiliateCommissionRate = affiliateCommissionRate;
        // Phase 2 Stripe fields — initialized to null (set via setters after construction)
        this.stripeBookingIntentId = null;
        this.stripeDepositIntentId = null;
        this.depositAmount = null;
        this.depositCapturedAt = null;
        this.depositCaptureAmount = null;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.statusHistory = new ArrayList<>(statusHistory != null ? statusHistory : List.of());
        this.reservedAt = reservedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.cancelledAt = cancelledAt;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates a new booking in REQUEST status.
     * <p>
     * This is the entry point when a guest submits a booking request via referral
     * link.
     * </p>
     *
     * @param referralLinkId the referral link used
     * @param propertyId     the property being booked
     * @param bookingDates   check-in and check-out dates
     * @param guestInfo      guest information
     * @return a new Booking in REQUEST status
     */
    public static Booking createRequest(
            Long referralLinkId,
            Long propertyId,
            BookingDates bookingDates,
            GuestInfo guestInfo) {
        LocalDateTime now = LocalDateTime.now();
        return new Booking(
                null, // ID assigned by persistence layer
                referralLinkId,
                propertyId,
                bookingDates,
                guestInfo,
                null, // No Airbnb code yet
                null, // No amount in REQUEST
                null, // No host commission rate in REQUEST
                null, // No affiliate commission rate in REQUEST
                BookingStatus.REQUEST,
                new ArrayList<>(),
                null, null, null, null,
                now,
                now);
    }

    /**
     * Adds the initial status history entry after the booking has been persisted.
     * <p>
     * Must be called AFTER the booking has been saved and assigned a database ID.
     * This avoids the chicken-and-egg problem where BookingStatusHistory requires
     * a non-null bookingId, but the ID is only assigned upon persistence.
     * </p>
     */
    public void addInitialStatusHistory() {
        if (this.id == null) {
            throw new IllegalStateException(
                    "Cannot add initial status history before booking is persisted (ID is null)");
        }
        addStatusHistory(
                BookingStatusHistory.automatic(
                        this.id,
                        null, // No previous status
                        BookingStatus.REQUEST,
                        "Initial booking request created"));
    }

    /**
     * Reconstitution constructor for persistence layer.
     * Used when loading existing booking from database.
     */
    public static Booking reconstitute(
            Long id,
            Long referralLinkId,
            Long propertyId,
            BookingDates bookingDates,
            GuestInfo guestInfo,
            String airbnbConfirmationCode,
            Money totalAmount,
            BigDecimal hostCommissionRate,
            BigDecimal affiliateCommissionRate,
            BookingStatus status,
            List<BookingStatusHistory> statusHistory,
            LocalDateTime reservedAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new Booking(
                id,
                referralLinkId,
                propertyId,
                bookingDates,
                guestInfo,
                airbnbConfirmationCode,
                totalAmount,
                hostCommissionRate,
                affiliateCommissionRate,
                status,
                statusHistory,
                reservedAt,
                startedAt,
                finishedAt,
                cancelledAt,
                createdAt,
                updatedAt);
    }

    // ========== BUSINESS LOGIC: STATE TRANSITIONS ==========

    /**
     * Transitions booking from REQUEST to RESERVED.
     * <p>
     * Called when ADMIN confirms the booking in Airbnb.
     * REQUIRES: Airbnb confirmation code, total amount, and commission rates
     * (snapshot).
     * </p>
     *
     * @param airbnbCode              Airbnb confirmation code
     * @param amount                  total booking amount
     * @param hostCommissionRate      rate snapshot for host
     * @param affiliateCommissionRate rate snapshot for affiliate (nullable)
     * @param adminId                 ID of admin who confirmed
     * @throws InvalidBookingStatusTransitionException if current status is not
     *                                                 REQUEST
     */
    public void reserve(String airbnbCode, Money amount, BigDecimal hostCommissionRate,
            BigDecimal affiliateCommissionRate, Long adminId) {
        validateTransition(BookingStatus.RESERVED);
        validateReserveParameters(airbnbCode, amount, hostCommissionRate, adminId);

        this.airbnbConfirmationCode = airbnbCode;
        this.totalAmount = amount;
        this.hostCommissionRate = hostCommissionRate;
        this.affiliateCommissionRate = affiliateCommissionRate;
        this.status = BookingStatus.RESERVED;
        this.reservedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.manual(
                        this.id,
                        BookingStatus.REQUEST,
                        BookingStatus.RESERVED,
                        adminId,
                        String.format("Booking confirmed in Airbnb with code: %s", airbnbCode)));
    }

    /**
     * Updates booking dates (check-in and check-out).
     * <p>
     * Used when dates changed in Airbnb after initial request.
     * Creates a new BookingDates value object with updated dates.
     * </p>
     *
     * @param newCheckInDate  new check-in date
     * @param newCheckOutDate new check-out date
     * @throws IllegalArgumentException if dates are invalid
     */
    public void updateDates(LocalDate newCheckInDate, LocalDate newCheckOutDate) {
        Objects.requireNonNull(newCheckInDate, "New check-in date cannot be null");
        Objects.requireNonNull(newCheckOutDate, "New check-out date cannot be null");

        // Create new BookingDates value object (uses reconstitute to skip future date
        // validation)
        this.bookingDates = BookingDates.reconstitute(newCheckInDate, newCheckOutDate);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Transitions booking from RESERVED to IN_PROGRESS.
     * <p>
     * Automatically triggered on check-in date.
     * </p>
     *
     * @throws InvalidBookingStatusTransitionException if current status is not
     *                                                 RESERVED
     */
    public void startStay() {
        validateTransition(BookingStatus.IN_PROGRESS);

        this.status = BookingStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.automatic(
                        this.id,
                        BookingStatus.RESERVED,
                        BookingStatus.IN_PROGRESS,
                        "Guest checked in automatically on check-in date"));
    }

    /**
     * Transitions booking from IN_PROGRESS to FINISHED.
     * <p>
     * Automatically triggered on check-out date.
     * <strong>TRIGGER:</strong> This should publish a BookingFinishedEvent
     * which will create a commission record.
     * </p>
     *
     * @throws InvalidBookingStatusTransitionException if current status is not
     *                                                 IN_PROGRESS
     */
    public void finish() {
        validateTransition(BookingStatus.FINISHED);

        this.status = BookingStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.automatic(
                        this.id,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.FINISHED,
                        "Guest checked out automatically on check-out date"));

        // NOTE: Domain event publishing is handled by the use case layer
        // The use case will publish BookingFinishedEvent after persisting this change
    }

    /**
     * Cancels the booking.
     * <p>
     * Can be called from REQUEST, RESERVED, or IN_PROGRESS status.
     * Cannot cancel FINISHED bookings (use dispute instead).
     * </p>
     *
     * @param cancelledBy user who cancelled (admin, host, or guest)
     * @param reason      cancellation reason
     * @throws InvalidBookingStatusTransitionException if booking is FINISHED,
     *                                                 CANCELLED, or DISPUTED
     */
    public void cancel(Long cancelledBy, String reason) {
        validateTransition(BookingStatus.CANCELLED);
        Objects.requireNonNull(cancelledBy, "Cancelled by user ID is required");

        BookingStatus previousStatus = this.status;
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.manual(
                        this.id,
                        previousStatus,
                        BookingStatus.CANCELLED,
                        cancelledBy,
                        reason != null ? reason : "Booking cancelled"));
    }

    /**
     * Marks booking as DISPUTED.
     * <p>
     * Only applicable to FINISHED bookings when there's a complaint or issue.
     * </p>
     *
     * @param disputedBy user who raised the dispute
     * @param reason     dispute reason
     * @throws InvalidBookingStatusTransitionException if booking is not FINISHED
     */
    public void dispute(Long disputedBy, String reason) {
        validateTransition(BookingStatus.DISPUTED);
        Objects.requireNonNull(disputedBy, "Disputed by user ID is required");
        Objects.requireNonNull(reason, "Dispute reason is required");

        this.status = BookingStatus.DISPUTED;
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.manual(
                        this.id,
                        BookingStatus.FINISHED,
                        BookingStatus.DISPUTED,
                        disputedBy,
                        reason));
    }

    // ========== VALIDATION METHODS ==========

    private void validateTransition(BookingStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidBookingStatusTransitionException(
                    String.format("Cannot transition from %s to %s", status, targetStatus));
        }
    }

    private void validateReserveParameters(String airbnbCode, Money amount, BigDecimal hostCommissionRate,
            Long adminId) {
        if (airbnbCode == null || airbnbCode.isBlank()) {
            throw new IllegalArgumentException("Airbnb confirmation code is required to reserve booking");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Valid total amount is required to reserve booking");
        }
        if (hostCommissionRate == null || hostCommissionRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid host commission rate is required to reserve booking");
        }
        if (adminId == null) {
            throw new IllegalArgumentException("Admin ID is required to reserve booking");
        }
    }

    private void addStatusHistory(BookingStatusHistory history) {
        this.statusHistory.add(history);
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
     * Checks if booking was cancelled.
     */
    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /**
     * Checks if booking is currently disputed.
     */
    public boolean isDisputed() {
        return status == BookingStatus.DISPUTED;
    }

    /**
     * Checks if guest should be checked in (check-in date due).
     */
    public boolean shouldStartStay() {
        return status == BookingStatus.RESERVED && bookingDates.isCheckInDue();
    }

    /**
     * Checks if guest should be checked out (check-out date due).
     */
    public boolean shouldFinish() {
        return status == BookingStatus.IN_PROGRESS && bookingDates.isCheckOutDue();
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

    public BookingDates getBookingDates() {
        return bookingDates;
    }

    public GuestInfo getGuestInfo() {
        return guestInfo;
    }

    public String getAirbnbConfirmationCode() {
        return airbnbConfirmationCode;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getHostCommissionRate() {
        return hostCommissionRate;
    }

    public BigDecimal getAffiliateCommissionRate() {
        return affiliateCommissionRate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public List<BookingStatusHistory> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ========== CONVENIENCE ACCESSORS ==========

    /**
     * Convenience method: returns check-in date from BookingDates.
     */
    public LocalDate getCheckInDate() {
        return bookingDates.getCheckInDate();
    }

    /**
     * Convenience method: returns check-out date from BookingDates.
     */
    public LocalDate getCheckOutDate() {
        return bookingDates.getCheckOutDate();
    }

    /**
     * Convenience method: returns price per night (total / nights).
     * Returns totalAmount if the booking spans a single night or totalAmount is null.
     */
    public Money getPricePerNight() {
        if (totalAmount == null) {
            return null;
        }
        long nights = java.time.temporal.ChronoUnit.DAYS.between(
                bookingDates.getCheckInDate(), bookingDates.getCheckOutDate());
        if (nights <= 0) {
            return totalAmount;
        }
        return totalAmount.multiply(java.math.BigDecimal.ONE.divide(
                java.math.BigDecimal.valueOf(nights), 2, java.math.RoundingMode.HALF_UP));
    }

    // ========== PHASE 2: STRIPE PAYMENT FIELDS ==========

    public String getStripeBookingIntentId() { return stripeBookingIntentId; }
    public void setStripeBookingIntentId(String stripeBookingIntentId) {
        this.stripeBookingIntentId = stripeBookingIntentId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getStripeDepositIntentId() { return stripeDepositIntentId; }
    public void setStripeDepositIntentId(String stripeDepositIntentId) {
        this.stripeDepositIntentId = stripeDepositIntentId;
        this.updatedAt = LocalDateTime.now();
    }

    public Money getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Money depositAmount) {
        this.depositAmount = depositAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getDepositCapturedAt() { return depositCapturedAt; }
    public BigDecimal getDepositCaptureAmount() { return depositCaptureAmount; }

    /** For reconstitution from persistence only. */
    public void setDepositCapturedAt(LocalDateTime depositCapturedAt) {
        this.depositCapturedAt = depositCapturedAt;
    }

    /** For reconstitution from persistence only. */
    public void setDepositCaptureAmount(BigDecimal depositCaptureAmount) {
        this.depositCaptureAmount = depositCaptureAmount;
    }

    public LocalDateTime getPaymentReleasedAt() { return paymentReleasedAt; }
    public void setPaymentReleasedAt(LocalDateTime paymentReleasedAt) {
        this.paymentReleasedAt = paymentReleasedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    // Phase 4 — Social Commerce
    public Long getContentPostId() { return contentPostId; }
    public void setContentPostId(Long contentPostId) { this.contentPostId = contentPostId; }

    /**
     * Records a security deposit capture event (damage reported by host).
     *
     * @param capturedAmountCents amount captured in cents (null = full amount)
     * @param damageDescription   description of damage
     */
    public void recordDepositCapture(Long capturedAmountCents, String damageDescription) {
        this.depositCapturedAt = LocalDateTime.now();
        if (capturedAmountCents != null && depositAmount != null) {
            this.depositCaptureAmount = java.math.BigDecimal.valueOf(capturedAmountCents)
                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else if (depositAmount != null) {
            this.depositCaptureAmount = depositAmount.getAmount();
        }
        this.updatedAt = LocalDateTime.now();

        addStatusHistory(
                BookingStatusHistory.automatic(
                        this.id,
                        this.status,
                        this.status,
                        "Security deposit captured: " + (damageDescription != null ? damageDescription : "damage")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
                "Booking{id=%d, property=%d, dates=%s, status=%s, guest=%s, amount=%s}",
                id, propertyId, bookingDates, status, guestInfo, totalAmount);
    }
}
