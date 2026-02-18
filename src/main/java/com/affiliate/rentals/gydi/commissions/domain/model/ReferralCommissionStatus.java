package com.affiliate.rentals.gydi.commissions.domain.model;

/**
 * Status lifecycle for referral commissions (platform PAYS referrer/affiliate).
 * <p>
 * Referral commissions are paid BIWEEKLY (days 1 and 15) after a 7-day dispute period.
 * This is the platform's expense.
 * </p>
 * <p>
 * State Transitions:
 * <pre>
 * WAITING_HOST_CHARGE → APPROVED (when host commission is successfully CHARGED)
 * WAITING_HOST_CHARGE → WITHHELD (when host commission reaches max retries and is permanently failed)
 * WAITING_HOST_CHARGE → CANCELLED (if booking is cancelled)
 * PENDING → APPROVED (after 7-day dispute period, legacy flow) → PAID (on payment date)
 * PENDING → CANCELLED (if booking cancelled or disputed during dispute period)
 * APPROVED → WITHHELD (if investigation needed)
 * </pre>
 * </p>
 */
public enum ReferralCommissionStatus {
    /**
     * Commission created but waiting for the host commission to be charged first.
     * The referrer cannot be paid until the platform has collected from the host.
     * Transitions to APPROVED when host commission is CHARGED, or to WITHHELD when
     * host charge fails definitively after max retries.
     */
    WAITING_HOST_CHARGE("Waiting for host charge confirmation"),

    /**
     * Commission created, in 7-day dispute protection period (legacy flow).
     * Cannot be paid until dispute period ends.
     */
    PENDING("In dispute period"),

    /**
     * Dispute period ended, ready for payment on next scheduled date (1st or 15th).
     */
    APPROVED("Ready for payment"),

    /**
     * Successfully paid to referrer's Stripe Connect account.
     */
    PAID("Successfully paid"),

    /**
     * Commission cancelled (booking cancelled or disputed before payment).
     */
    CANCELLED("Commission cancelled"),

    /**
     * Payment withheld due to investigation or policy violation.
     */
    WITHHELD("Payment withheld"),

    /**
     * Booking is under dispute, commission frozen.
     */
    DISPUTED("Under dispute");

    private final String description;

    ReferralCommissionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if commission can be approved via the dispute-period flow (PENDING → APPROVED).
     * WAITING_HOST_CHARGE commissions are approved via approveAfterHostCharged(), not this path.
     */
    public boolean canApprove() {
        return this == PENDING;
    }

    /**
     * Checks if commission can be approved after host charge (WAITING_HOST_CHARGE → APPROVED).
     * This is the primary approval path for new commissions.
     */
    public boolean canApproveFromHostCharge() {
        return this == WAITING_HOST_CHARGE;
    }

    /**
     * Checks if commission can be paid (transition from APPROVED to PAID).
     */
    public boolean canPay() {
        return this == APPROVED;
    }

    /**
     * Checks if commission can be cancelled.
     */
    public boolean canCancel() {
        return this == WAITING_HOST_CHARGE || this == PENDING || this == APPROVED;
    }

    /**
     * Checks if this is a terminal state (no further processing).
     */
    public boolean isTerminal() {
        return this == PAID || this == CANCELLED;
    }

    /**
     * Checks if commission was successfully paid.
     */
    public boolean isSuccessful() {
        return this == PAID;
    }
}
