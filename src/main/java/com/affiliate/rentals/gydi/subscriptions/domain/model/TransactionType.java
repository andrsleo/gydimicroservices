package com.affiliate.rentals.gydi.subscriptions.domain.model;

/**
 * Enumeration of subscription transaction types.
 *
 * <p>This enum represents the different types of operations that can occur
 * during a subscription's lifecycle.</p>
 *
 * @author GYDI Development Team
 */
public enum TransactionType {
    /**
     * Initial subscription to a plan (from FREE to paid plan, or first-time subscription).
     */
    INITIAL_SUBSCRIPTION,

    /**
     * Upgrade from a lower-tier plan to a higher-tier plan.
     */
    UPGRADE,

    /**
     * Automatic or manual renewal of the current plan.
     */
    RENEWAL,

    /**
     * Downgrade from a higher-tier plan to a lower-tier plan.
     */
    DOWNGRADE,

    /**
     * Cancellation of a paid subscription.
     */
    CANCELLATION,

    /**
     * Reactivation of a previously canceled or expired subscription.
     */
    REACTIVATION;

    /**
     * Checks if this transaction type involves a payment.
     *
     * @return {@code true} if transaction requires payment, {@code false} otherwise
     */
    public boolean requiresPayment() {
        return this == INITIAL_SUBSCRIPTION || this == UPGRADE || this == RENEWAL || this == REACTIVATION;
    }

    /**
     * Checks if this transaction type changes the plan tier.
     *
     * @return {@code true} if plan tier changes, {@code false} otherwise
     */
    public boolean changesPlanTier() {
        return this == UPGRADE || this == DOWNGRADE || this == INITIAL_SUBSCRIPTION;
    }

    /**
     * Checks if this transaction type is a plan change (upgrade or downgrade).
     *
     * @return {@code true} if transaction is a plan change, {@code false} otherwise
     */
    public boolean isPlanChange() {
        return this == UPGRADE || this == DOWNGRADE;
    }
}
