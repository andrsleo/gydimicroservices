package com.affiliate.rentals.gydi.commissions.domain.model;

/**
 * Status lifecycle for host commissions (platform CHARGES host).
 * <p>
 * Host commissions are charged IMMEDIATELY when booking transitions to FINISHED.
 * This is the platform's revenue stream.
 * </p>
 * <p>
 * State Transitions:
 * <pre>
 * PENDING → PROCESSING → CHARGED (success)
 * PENDING → FAILED (retry up to 3 times)
 * FAILED → WITHHELD (after max retries exceeded, requires manual intervention)
 * CHARGED → REFUNDED (if booking disputed)
 * </pre>
 */
public enum HostCommissionStatus {
    /**
     * Commission record created, awaiting charge attempt.
     */
    PENDING("Awaiting charge"),

    /**
     * Stripe payment intent is being processed.
     */
    PROCESSING("Charging in progress"),

    /**
     * Successfully charged to host's payment method.
     */
    CHARGED("Successfully charged"),

    /**
     * Charge failed (insufficient funds, card declined, etc.).
     * System will retry up to 3 times with exponential backoff.
     */
    FAILED("Charge failed"),

    /**
     * Charge was refunded (due to booking dispute or cancellation).
     */
    REFUNDED("Charge refunded"),

    /**
     * Booking is under dispute, commission on hold.
     */
    DISPUTED("Under dispute"),

    /**
     * Charge permanently withheld after max retry attempts exceeded.
     * Requires manual intervention by operations team.
     */
    WITHHELD("Charge permanently withheld - max retries exceeded");

    private final String description;

    HostCommissionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if status allows charge retry.
     */
    public boolean canRetry() {
        return this == PENDING || this == FAILED;
    }

    /**
     * Checks if this is a terminal state (no further processing).
     */
    public boolean isTerminal() {
        return this == CHARGED || this == REFUNDED || this == WITHHELD;
    }

    /**
     * Checks if commission was successfully charged.
     */
    public boolean isSuccessful() {
        return this == CHARGED;
    }
}
