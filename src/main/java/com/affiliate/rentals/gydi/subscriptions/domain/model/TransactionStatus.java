package com.affiliate.rentals.gydi.subscriptions.domain.model;

/**
 * Enumeration of transaction processing statuses.
 *
 * <p>This enum represents the various states a payment transaction can be in
 * throughout its processing lifecycle.</p>
 *
 * @author GYDI Development Team
 */
public enum TransactionStatus {
    /**
     * Transaction has been created but not yet processed.
     */
    PENDING,

    /**
     * Transaction is currently being processed by the payment gateway.
     */
    PROCESSING,

    /**
     * Transaction has been successfully completed and payment confirmed.
     */
    COMPLETED,

    /**
     * Transaction failed due to payment rejection, insufficient funds, or other errors.
     */
    FAILED,

    /**
     * Transaction was completed but has been refunded to the user.
     */
    REFUNDED,

    /**
     * Transaction was canceled before completion (by user or system).
     */
    CANCELED;

    /**
     * Checks if the transaction is in a final state (no further processing).
     *
     * @return {@code true} if transaction is in a final state, {@code false} otherwise
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELED;
    }

    /**
     * Checks if the transaction was successful.
     *
     * @return {@code true} if transaction succeeded, {@code false} otherwise
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Checks if the transaction is still in progress.
     *
     * @return {@code true} if transaction is being processed, {@code false} otherwise
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Checks if the transaction failed or was canceled.
     *
     * @return {@code true} if transaction was not successful, {@code false} otherwise
     */
    public boolean isUnsuccessful() {
        return this == FAILED || this == CANCELED;
    }
}
