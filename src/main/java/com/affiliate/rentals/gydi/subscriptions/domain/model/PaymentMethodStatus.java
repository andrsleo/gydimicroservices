package com.affiliate.rentals.gydi.subscriptions.domain.model;

/**
 * Enum representing the lifecycle status of a payment method.
 *
 * <p>This enum defines all possible states a payment method can be in throughout
 * its lifecycle, supporting soft delete functionality and proper audit tracking.</p>
 *
 * <p>Status Transitions:</p>
 * <ul>
 *   <li>ACTIVE → INACTIVE (soft delete via user action)</li>
 *   <li>ACTIVE → EXPIRED (automatic when card expires)</li>
 *   <li>ACTIVE → FAILED (when gateway validation fails)</li>
 *   <li>ACTIVE → REPLACED (when user adds new card and removes old one)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public enum PaymentMethodStatus {

    /**
     * Payment method is active and available for processing payments.
     * This is the default state for newly created payment methods.
     */
    ACTIVE,

    /**
     * Payment method has been deactivated by the user (soft delete).
     * The record is retained for audit purposes but cannot be used for payments.
     * This is the primary state for implementing soft delete functionality.
     */
    INACTIVE,

    /**
     * Payment method has expired based on its expiration date.
     * Automatically set when card expiration date has passed.
     * User should be prompted to update or add a new payment method.
     */
    EXPIRED,

    /**
     * Payment method failed validation by the payment gateway.
     * This can occur when:
     * - Card is declined
     * - Insufficient funds
     * - Bank rejects the transaction
     * - Gateway detects suspicious activity
     */
    FAILED,

    /**
     * Payment method has been replaced by a newer payment method.
     * Used when user adds a new card and explicitly replaces an existing one.
     * Helps maintain audit trail while keeping the database clean.
     */
    REPLACED;

    /**
     * Checks if this status represents an active, usable payment method.
     *
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isUsable() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status represents a deleted/deactivated payment method.
     *
     * @return true if status is INACTIVE, false otherwise
     */
    public boolean isDeleted() {
        return this == INACTIVE;
    }

    /**
     * Returns a human-readable display name for this status.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case EXPIRED -> "Expired";
            case FAILED -> "Failed";
            case REPLACED -> "Replaced";
        };
    }
}
