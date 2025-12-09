package com.affiliate.rentals.gydi.payment.domain.model;

/**
 * Payment status enum with state transition validation.
 * <p>
 * State machine:
 * PENDING → PROCESSING → SUCCESS or FAILED
 * PENDING/PROCESSING → CANCELED
 * SUCCESS/FAILED/CANCELED → terminal states
 * </p>
 */
public enum PaymentStatus {

    /**
     * Initial state - payment record created, awaiting processing.
     */
    PENDING,

    /**
     * Payment is being processed.
     */
    PROCESSING,

    /**
     * Payment successfully processed and confirmed.
     */
    SUCCESS,

    /**
     * Payment processing failed.
     */
    FAILED,

    /**
     * Payment canceled before completion.
     */
    CANCELED;

    /**
     * Validates if a transition to target status is allowed.
     */
    public boolean canTransitionTo(PaymentStatus targetStatus) {
        return switch (this) {
            case PENDING -> targetStatus == PROCESSING || targetStatus == CANCELED;
            case PROCESSING -> targetStatus == SUCCESS || targetStatus == FAILED || targetStatus == CANCELED;
            case SUCCESS, FAILED, CANCELED -> false; // Terminal states
        };
    }

    /**
     * Checks if this is a terminal state (no further transitions allowed).
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELED;
    }

    /**
     * Checks if this is a processing state (PENDING or PROCESSING).
     */
    public boolean isProcessing() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Checks if payment is complete (SUCCESS).
     */
    public boolean isComplete() {
        return this == SUCCESS;
    }
}
