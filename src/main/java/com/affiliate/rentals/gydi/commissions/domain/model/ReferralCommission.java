package com.affiliate.rentals.gydi.commissions.domain.model;

import com.affiliate.rentals.gydi.commissions.domain.exception.InvalidCommissionStateException;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.CommissionAmount;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.DisputePeriod;
import com.affiliate.rentals.gydi.commissions.domain.model.vo.PaymentSchedule;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Referral Commission Aggregate Root.
 * <p>
 * Represents commission that the platform PAYS to the referrer (affiliate) for successful referrals.
 * This is the platform's marketing/referral expense.
 * </p>
 * <p>
 * Lifecycle: WAITING_HOST_CHARGE → APPROVED → PAID (on 1st or 15th)
 * </p>
 * <p>
 * <strong>PURE DOMAIN MODEL</strong> - No framework dependencies (no JPA, Spring, etc.)
 * </p>
 */
public class ReferralCommission {
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Identity
    private Long id;

    // Associations
    private final Long bookingId;
    private final Long affiliateId;

    // Commission snapshot (at time of booking completion)
    private final String affiliatePlan;
    private final CommissionAmount amount;

    // Payment scheduling
    private final PaymentSchedule paymentSchedule;
    private final DisputePeriod disputePeriod;

    // Stripe payout processing
    private String stripeTransferId;
    private String stripePayoutId;

    // State
    private ReferralCommissionStatus status;
    private LocalDateTime paidAt;

    // Retry logic
    private String failureReason;
    private int attemptCount;
    private LocalDateTime lastAttemptAt;

    // Audit
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private constructor - use factory methods.
     */
    private ReferralCommission(
        Long id,
        Long bookingId,
        Long affiliateId,
        String affiliatePlan,
        CommissionAmount amount,
        PaymentSchedule paymentSchedule,
        DisputePeriod disputePeriod,
        String stripeTransferId,
        String stripePayoutId,
        ReferralCommissionStatus status,
        LocalDateTime paidAt,
        String failureReason,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.bookingId = Objects.requireNonNull(bookingId, "Booking ID cannot be null");
        this.affiliateId = Objects.requireNonNull(affiliateId, "Affiliate ID cannot be null");
        this.affiliatePlan = Objects.requireNonNull(affiliatePlan, "Affiliate plan cannot be null");
        this.amount = Objects.requireNonNull(amount, "Commission amount cannot be null");
        this.paymentSchedule = Objects.requireNonNull(paymentSchedule, "Payment schedule cannot be null");
        this.disputePeriod = Objects.requireNonNull(disputePeriod, "Dispute period cannot be null");
        this.stripeTransferId = stripeTransferId;
        this.stripePayoutId = stripePayoutId;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.paidAt = paidAt;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.updatedAt = Objects.requireNonNullElse(updatedAt, LocalDateTime.now());
    }

    /**
     * Factory method: Creates a new referral commission in WAITING_HOST_CHARGE status.
     * <p>
     * The referral commission starts in WAITING_HOST_CHARGE because it can only be paid
     * after the host commission is successfully charged. This enforces the business rule
     * that the platform never pays out what it has not collected.
     * </p>
     */
    public static ReferralCommission create(
        Long bookingId,
        Long affiliateId,
        String affiliatePlan,
        CommissionAmount amount,
        LocalDateTime bookingFinishedAt
    ) {
        DisputePeriod disputePeriod = DisputePeriod.calculate(bookingFinishedAt);
        PaymentSchedule paymentSchedule = PaymentSchedule.calculate(
            disputePeriod.getDisputePeriodEndsAt()
        );

        LocalDateTime now = LocalDateTime.now();
        return new ReferralCommission(
            null, // ID assigned by persistence layer
            bookingId,
            affiliateId,
            affiliatePlan,
            amount,
            paymentSchedule,
            disputePeriod,
            null, // No Stripe IDs yet
            null,
            ReferralCommissionStatus.WAITING_HOST_CHARGE,
            null, // Not paid yet
            null, // No failure
            0, // No attempts yet
            null,
            now,
            now
        );
    }

    /**
     * Reconstitution constructor for persistence layer.
     */
    public static ReferralCommission reconstitute(
        Long id,
        Long bookingId,
        Long affiliateId,
        String affiliatePlan,
        CommissionAmount amount,
        PaymentSchedule paymentSchedule,
        DisputePeriod disputePeriod,
        String stripeTransferId,
        String stripePayoutId,
        ReferralCommissionStatus status,
        LocalDateTime paidAt,
        String failureReason,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return new ReferralCommission(
            id, bookingId, affiliateId, affiliatePlan, amount,
            paymentSchedule, disputePeriod,
            stripeTransferId, stripePayoutId, status, paidAt,
            failureReason, attemptCount, lastAttemptAt,
            createdAt, updatedAt
        );
    }

    // ========== BUSINESS LOGIC: STATE TRANSITIONS ==========

    /**
     * Transitions commission to PENDING after host has been successfully charged,
     * when the affiliate has NOT yet completed Stripe Connect onboarding.
     * <p>
     * Transitions from WAITING_HOST_CHARGE to PENDING.
     * Called by ChargeHostCommissionUseCase when the host charge succeeds but the
     * affiliate does not yet have a fully onboarded Stripe Connect account.
     * The commission will stay PENDING until a scheduler or manual trigger moves it
     * to APPROVED once the affiliate completes onboarding.
     * </p>
     */
    public void pendingAfterHostCharge() {
        if (status != ReferralCommissionStatus.WAITING_HOST_CHARGE) {
            throw new InvalidCommissionStateException(
                "Cannot transition to PENDING from status: " + status +
                ". Expected WAITING_HOST_CHARGE."
            );
        }

        this.status = ReferralCommissionStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Approves commission after host has been successfully charged.
     * <p>
     * Transitions from WAITING_HOST_CHARGE to APPROVED.
     * Called by ChargeHostCommissionUseCase when the host charge succeeds.
     * This is the primary approval path for new commissions.
     * </p>
     */
    public void approveAfterHostCharged() {
        if (!status.canApproveFromHostCharge()) {
            throw new InvalidCommissionStateException(
                "Cannot approve after host charge from status: " + status +
                ". Expected WAITING_HOST_CHARGE."
            );
        }

        this.status = ReferralCommissionStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Withholds commission because the host charge failed definitively (max retries exceeded).
     * <p>
     * Transitions from WAITING_HOST_CHARGE to WITHHELD.
     * Called by CommissionPaymentScheduler when the host commission reaches max retries.
     * The referrer will NOT be paid because the platform never collected from the host.
     * </p>
     */
    public void withholdDueToHostChargeFailure(String reason) {
        if (status != ReferralCommissionStatus.WAITING_HOST_CHARGE) {
            throw new InvalidCommissionStateException(
                "Cannot withhold due to host charge failure from status: " + status +
                ". Expected WAITING_HOST_CHARGE."
            );
        }

        this.status = ReferralCommissionStatus.WITHHELD;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Approves commission for payment (after dispute period ends, legacy flow).
     */
    public void approve() {
        if (!status.canApprove()) {
            throw new InvalidCommissionStateException(
                "Cannot approve commission in status: " + status
            );
        }

        if (disputePeriod.isActive()) {
            throw new InvalidCommissionStateException(
                "Cannot approve commission while dispute period is active"
            );
        }

        this.status = ReferralCommissionStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks commission as successfully PAID.
     */
    public void markAsPaid(String stripeTransferId, String stripePayoutId) {
        if (!status.canPay()) {
            throw new InvalidCommissionStateException(
                "Cannot mark as paid from status: " + status
            );
        }

        this.status = ReferralCommissionStatus.PAID;
        this.stripeTransferId = stripeTransferId;
        this.stripePayoutId = stripePayoutId;
        this.paidAt = LocalDateTime.now();
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Records payment failure.
     */
    public void recordPaymentFailure(String failureReason) {
        if (status != ReferralCommissionStatus.APPROVED) {
            throw new InvalidCommissionStateException(
                "Can only record failure for APPROVED commissions"
            );
        }

        this.failureReason = failureReason;
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // After 3 failed attempts, withhold commission for manual review
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            this.status = ReferralCommissionStatus.WITHHELD;
        }
    }

    /**
     * Cancels the commission (booking cancelled or disputed).
     */
    public void cancel(String cancellationReason) {
        if (!status.canCancel()) {
            throw new InvalidCommissionStateException(
                "Cannot cancel commission in status: " + status
            );
        }

        this.status = ReferralCommissionStatus.CANCELLED;
        this.failureReason = cancellationReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Withholds commission for investigation.
     */
    public void withhold(String withholdReason) {
        if (status == ReferralCommissionStatus.PAID || status == ReferralCommissionStatus.CANCELLED) {
            throw new InvalidCommissionStateException(
                "Cannot withhold commission in terminal status: " + status
            );
        }

        this.status = ReferralCommissionStatus.WITHHELD;
        this.failureReason = withholdReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks commission as disputed.
     */
    public void markAsDisputed(String disputeReason) {
        if (status == ReferralCommissionStatus.PAID) {
            throw new InvalidCommissionStateException(
                "Cannot dispute a paid commission"
            );
        }

        this.status = ReferralCommissionStatus.DISPUTED;
        this.failureReason = disputeReason;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== QUERY METHODS ==========

    /**
     * Checks if dispute period is still active.
     */
    public boolean isDisputePeriodActive() {
        return disputePeriod.isActive();
    }

    /**
     * Checks if payment is due today.
     */
    public boolean isPaymentDue() {
        return status == ReferralCommissionStatus.APPROVED &&
               !disputePeriod.isActive() &&
               paymentSchedule.isDue();
    }

    /**
     * Checks if commission is ready for approval.
     */
    public boolean isReadyForApproval() {
        return status == ReferralCommissionStatus.PENDING &&
               disputePeriod.hasEnded();
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

    public Long getAffiliateId() {
        return affiliateId;
    }

    public String getAffiliatePlan() {
        return affiliatePlan;
    }

    public CommissionAmount getAmount() {
        return amount;
    }

    public PaymentSchedule getPaymentSchedule() {
        return paymentSchedule;
    }

    public DisputePeriod getDisputePeriod() {
        return disputePeriod;
    }

    public String getStripeTransferId() {
        return stripeTransferId;
    }

    public String getStripePayoutId() {
        return stripePayoutId;
    }

    public ReferralCommissionStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
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
        ReferralCommission that = (ReferralCommission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "ReferralCommission{id=%d, booking=%d, affiliate=%d, amount=%s, status=%s}",
            id, bookingId, affiliateId, amount, status
        );
    }
}
