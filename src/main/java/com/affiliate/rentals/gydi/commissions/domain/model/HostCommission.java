package com.affiliate.rentals.gydi.commissions.domain.model;

import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidCommissionStateException;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Host Commission Aggregate Root.
 * <p>
 * Represents commission that the platform CHARGES to the host when a booking is completed.
 * This is the platform's primary revenue stream.
 * </p>
 * <p>
 * Lifecycle: PENDING → PROCESSING → CHARGED (or FAILED with retries)
 * </p>
 * <p>
 * <strong>PURE DOMAIN MODEL</strong> - No framework dependencies (no JPA, Spring, etc.)
 * </p>
 */
public class HostCommission {
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Identity
    private Long id;

    // Associations
    private final Long bookingId;
    private final Long hostId;

    // Commission snapshot (at time of booking completion)
    private final String hostPlan;
    private final CommissionAmount amount;

    // Stripe payment processing
    private String stripePaymentIntentId;
    private String stripeChargeId;

    // State
    private HostCommissionStatus status;
    private LocalDateTime chargedAt;

    // Retry logic
    private String failureReason;
    private int attemptCount;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRetryAt;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - use factory methods.
     */
    private HostCommission(
        Long id,
        Long bookingId,
        Long hostId,
        String hostPlan,
        CommissionAmount amount,
        String stripePaymentIntentId,
        String stripeChargeId,
        HostCommissionStatus status,
        LocalDateTime chargedAt,
        String failureReason,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime nextRetryAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.bookingId = Objects.requireNonNull(bookingId, "Booking ID cannot be null");
        this.hostId = Objects.requireNonNull(hostId, "Host ID cannot be null");
        this.hostPlan = Objects.requireNonNull(hostPlan, "Host plan cannot be null");
        this.amount = Objects.requireNonNull(amount, "Commission amount cannot be null");
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeChargeId = stripeChargeId;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.chargedAt = chargedAt;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates a new host commission in PENDING status.
     */
    public static HostCommission create(
        Long bookingId,
        Long hostId,
        String hostPlan,
        CommissionAmount amount
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new HostCommission(
            null, // ID assigned by persistence layer
            bookingId,
            hostId,
            hostPlan,
            amount,
            null, // No Stripe IDs yet
            null,
            HostCommissionStatus.PENDING,
            null, // Not charged yet
            null, // No failure
            0, // No attempts yet
            null,
            null,
            now,
            now
        );
    }

    /**
     * Reconstitution constructor for persistence layer.
     */
    public static HostCommission reconstitute(
        Long id,
        Long bookingId,
        Long hostId,
        String hostPlan,
        CommissionAmount amount,
        String stripePaymentIntentId,
        String stripeChargeId,
        HostCommissionStatus status,
        LocalDateTime chargedAt,
        String failureReason,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime nextRetryAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return new HostCommission(
            id, bookingId, hostId, hostPlan, amount,
            stripePaymentIntentId, stripeChargeId, status, chargedAt,
            failureReason, attemptCount, lastAttemptAt, nextRetryAt,
            createdAt, updatedAt
        );
    }

    // ========== BUSINESS LOGIC: STATE TRANSITIONS ==========

    /**
     * Marks commission as PROCESSING (charge in progress).
     */
    public void markAsProcessing(String stripePaymentIntentId) {
        if (!status.canRetry()) {
            throw new InvalidCommissionStateException(
                "Cannot process commission in status: " + status
            );
        }

        this.status = HostCommissionStatus.PROCESSING;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks commission as successfully CHARGED.
     */
    public void markAsCharged(String stripeChargeId) {
        markAsCharged(null, stripeChargeId, LocalDateTime.now());
    }

    /**
     * Marks commission as successfully CHARGED with full details.
     *
     * @param stripePaymentIntentId Stripe Payment Intent ID
     * @param stripeChargeId        Stripe Charge ID
     * @param chargedAt             Timestamp when charged
     */
    public void markAsCharged(String stripePaymentIntentId, String stripeChargeId, LocalDateTime chargedAt) {
        if (status != HostCommissionStatus.PROCESSING) {
            throw new InvalidCommissionStateException(
                "Cannot mark as charged from status: " + status
            );
        }

        this.status = HostCommissionStatus.CHARGED;
        if (stripePaymentIntentId != null) {
            this.stripePaymentIntentId = stripePaymentIntentId;
        }
        this.stripeChargeId = stripeChargeId;
        this.chargedAt = chargedAt != null ? chargedAt : LocalDateTime.now();
        this.attemptCount = 0; // Reset to 0 on successful charge (no failed attempts)
        this.failureReason = null;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks commission as FAILED with retry logic.
     */
    public void markAsFailed(String failureReason) {
        if (status != HostCommissionStatus.PROCESSING && status != HostCommissionStatus.PENDING) {
            throw new InvalidCommissionStateException(
                "Cannot mark as failed from status: " + status
            );
        }

        this.status = HostCommissionStatus.FAILED;
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();

        // Calculate next retry time (exponential backoff: 1min, 2min, 5min for testing)
        // TODO: restore to hours for production (plusHours: 1, 6, 24)
        if (canRetry()) {
            long minutesToWait = switch (attemptCount) {
                case 1 -> 1;
                case 2 -> 2;
                default -> 5;
            };
            this.nextRetryAt = LocalDateTime.now().plusMinutes(minutesToWait);
        } else {
            this.nextRetryAt = null; // Max retries reached
        }
    }

    /**
     * Marks commission as FAILED with full details.
     *
     * @param failureReason  Reason for failure
     * @param attemptCount   Number of attempts made
     * @param lastAttemptAt  Timestamp of last attempt
     * @param nextRetryAt    Timestamp for next retry
     */
    public void markAsFailed(String failureReason, int attemptCount, LocalDateTime lastAttemptAt, LocalDateTime nextRetryAt) {
        if (status != HostCommissionStatus.PROCESSING && status != HostCommissionStatus.PENDING) {
            throw new InvalidCommissionStateException(
                "Cannot mark as failed from status: " + status
            );
        }

        this.status = HostCommissionStatus.FAILED;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retry charge attempt.
     */
    public void retry() {
        if (!canRetry()) {
            throw new InvalidCommissionStateException(
                "Cannot retry commission: max attempts reached or invalid status"
            );
        }

        this.status = HostCommissionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Refunds a charged commission (due to booking dispute).
     */
    public void refund(String refundReason) {
        if (status != HostCommissionStatus.CHARGED) {
            throw new InvalidCommissionStateException(
                "Can only refund CHARGED commissions"
            );
        }

        this.status = HostCommissionStatus.REFUNDED;
        this.failureReason = refundReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Permanently withholds the commission after max retry attempts are exceeded.
     * <p>
     * Transitions from FAILED to WITHHELD. This is a terminal state that requires
     * manual intervention by the operations team. When this happens, the corresponding
     * affiliate commission must also be withheld (handled by CommissionPaymentScheduler).
     * </p>
     */
    public void withhold(String withholdReason) {
        if (status != HostCommissionStatus.FAILED) {
            throw new InvalidCommissionStateException(
                "Can only withhold FAILED commissions, current status: " + status
            );
        }

        this.status = HostCommissionStatus.WITHHELD;
        this.failureReason = withholdReason;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks commission as disputed.
     */
    public void markAsDisputed(String disputeReason) {
        if (status == HostCommissionStatus.REFUNDED) {
            throw new InvalidCommissionStateException(
                "Cannot dispute a refunded commission"
            );
        }

        this.status = HostCommissionStatus.DISPUTED;
        this.failureReason = disputeReason;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== QUERY METHODS ==========

    /**
     * Checks if commission can be retried.
     */
    public boolean canRetry() {
        return status.canRetry() && attemptCount < MAX_RETRY_ATTEMPTS;
    }

    /**
     * Checks if retry is due now.
     */
    public boolean isRetryDue() {
        return canRetry() &&
               nextRetryAt != null &&
               LocalDateTime.now().isAfter(nextRetryAt);
    }

    /**
     * Checks if commission is in terminal state.
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Cannot change commission ID once set");
        }
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getHostPlan() {
        return hostPlan;
    }

    public CommissionAmount getAmount() {
        return amount;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public String getStripeChargeId() {
        return stripeChargeId;
    }

    public HostCommissionStatus getStatus() {
        return status;
    }

    public LocalDateTime getChargedAt() {
        return chargedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
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
        HostCommission that = (HostCommission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "HostCommission{id=%d, booking=%d, host=%d, amount=%s, status=%s}",
            id, bookingId, hostId, amount, status
        );
    }
}
