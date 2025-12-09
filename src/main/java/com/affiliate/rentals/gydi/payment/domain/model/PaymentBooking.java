package com.affiliate.rentals.gydi.payment.domain.model;

import com.affiliate.rentals.gydi.payment.domain.exception.InvalidPaymentStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PaymentBooking Aggregate Root.
 * <p>
 * Represents a payment record for a completed booking with commission calculation.
 * Created automatically when a Booking transitions to FINISHED status.
 * </p>
 * <p>
 * <strong>Business Rules:</strong>
 * - Commission is calculated as: totalAmount × commissionPercentage
 * - Payment is initially PENDING
 * - Can transition through: PENDING → PROCESSING → SUCCESS/FAILED
 * - Can be CANCELED at any time before SUCCESS/FAILED
 * - Once SUCCESS, paidAt timestamp is automatically set
 * </p>
 */
public class PaymentBooking {

    private Long id;
    private final Long bookingId;
    private final Long referralLinkId;
    private final Long userId; // Affiliate who receives payment
    private final Money totalAmount;
    private final Money commissionAmount;
    private final BigDecimal commissionPercentage;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor for domain invariant enforcement.
     */
    private PaymentBooking(Long id,
                           Long bookingId,
                           Long referralLinkId,
                           Long userId,
                           Money totalAmount,
                           Money commissionAmount,
                           BigDecimal commissionPercentage,
                           PaymentStatus status,
                           LocalDateTime paidAt,
                           String failureReason,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        validateInvariants(bookingId, referralLinkId, userId, totalAmount, commissionAmount, commissionPercentage);

        this.id = id;
        this.bookingId = bookingId;
        this.referralLinkId = referralLinkId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.commissionAmount = commissionAmount;
        this.commissionPercentage = commissionPercentage;
        this.status = status;
        this.paidAt = paidAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method: Creates a new pending payment for a finished booking.
     */
    public static PaymentBooking createPending(Long bookingId,
                                                Long referralLinkId,
                                                Long userId,
                                                Money totalAmount,
                                                BigDecimal commissionPercentage) {
        Money commissionAmount = totalAmount.multiplyByPercentage(commissionPercentage);

        return new PaymentBooking(
            null, // ID assigned by persistence layer
            bookingId,
            referralLinkId,
            userId,
            totalAmount,
            commissionAmount,
            commissionPercentage,
            PaymentStatus.PENDING,
            null, // paidAt set when status → SUCCESS
            null, // no failure reason yet
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /**
     * Reconstitute from persistence (used by repository adapter).
     */
    public static PaymentBooking reconstitute(Long id,
                                               Long bookingId,
                                               Long referralLinkId,
                                               Long userId,
                                               Money totalAmount,
                                               Money commissionAmount,
                                               BigDecimal commissionPercentage,
                                               PaymentStatus status,
                                               LocalDateTime paidAt,
                                               String failureReason,
                                               LocalDateTime createdAt,
                                               LocalDateTime updatedAt) {
        return new PaymentBooking(
            id, bookingId, referralLinkId, userId,
            totalAmount, commissionAmount, commissionPercentage,
            status, paidAt, failureReason, createdAt, updatedAt
        );
    }

    // ========== STATE TRANSITIONS ==========

    /**
     * Transitions payment to PROCESSING status.
     */
    public void markAsProcessing() {
        if (!status.canTransitionTo(PaymentStatus.PROCESSING)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.PROCESSING);
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks payment as successfully completed.
     * Sets paidAt timestamp.
     */
    public void markAsSuccess() {
        if (!status.canTransitionTo(PaymentStatus.SUCCESS)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.SUCCESS);
        }
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks payment as failed with reason.
     */
    public void markAsFailed(String failureReason) {
        if (!status.canTransitionTo(PaymentStatus.FAILED)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.FAILED);
        }
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason is required when marking payment as failed");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancels payment before completion.
     */
    public void cancel(String reason) {
        if (!status.canTransitionTo(PaymentStatus.CANCELED)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.CANCELED);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        this.status = PaymentStatus.CANCELED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== VALIDATION ==========

    private void validateInvariants(Long bookingId,
                                     Long referralLinkId,
                                     Long userId,
                                     Money totalAmount,
                                     Money commissionAmount,
                                     BigDecimal commissionPercentage) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (referralLinkId == null || referralLinkId <= 0) {
            throw new IllegalArgumentException("Referral link ID must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (totalAmount == null || !totalAmount.isPositive()) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (commissionAmount == null || !commissionAmount.isPositive()) {
            throw new IllegalArgumentException("Commission amount must be positive");
        }
        if (commissionPercentage == null ||
            commissionPercentage.compareTo(BigDecimal.ZERO) <= 0 ||
            commissionPercentage.compareTo(BigDecimal.ONE) > 1) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 1");
        }
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getReferralLinkId() {
        return referralLinkId;
    }

    public Long getUserId() {
        return userId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getCommissionAmount() {
        return commissionAmount;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Only for persistence layer to set ID after save
    public void setId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Cannot change ID once set");
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentBooking that = (PaymentBooking) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PaymentBooking{" +
            "id=" + id +
            ", bookingId=" + bookingId +
            ", userId=" + userId +
            ", totalAmount=" + totalAmount +
            ", commissionAmount=" + commissionAmount +
            ", status=" + status +
            '}';
    }
}
