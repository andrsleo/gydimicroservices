package com.affiliate.rentals.gydi.subscriptions.domain.model;

/**
 * Enumeration of possible subscription statuses.
 *
 * <p>This enum represents the lifecycle states of a user's subscription.</p>
 *
 * <ul>
 *   <li>{@code ACTIVE} - Subscription is currently active and user has full access</li>
 *   <li>{@code EXPIRED} - Subscription has expired but can be renewed</li>
 *   <li>{@code CANCELED} - User has canceled their subscription</li>
 *   <li>{@code SUSPENDED} - Subscription has been suspended (e.g., payment failure, admin action)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public enum SubscriptionStatus {
    /**
     * Subscription is currently active with full access to features.
     */
    ACTIVE,

    /**
     * Subscription has expired due to non-payment or end of billing period.
     * Can be renewed.
     */
    EXPIRED,

    /**
     * Subscription has been canceled by the user or admin.
     * May require reactivation rather than renewal.
     */
    CANCELED,

    /**
     * Subscription has been suspended temporarily.
     * Usually due to payment issues or administrative action.
     */
    SUSPENDED;

    /**
     * Checks if the subscription status allows access to paid features.
     *
     * @return {@code true} if status is ACTIVE, {@code false} otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if the subscription can be renewed.
     *
     * @return {@code true} if status allows renewal, {@code false} otherwise
     */
    public boolean canBeRenewed() {
        return this == EXPIRED || this == CANCELED;
    }

    /**
     * Checks if the subscription requires immediate action.
     *
     * @return {@code true} if status requires attention, {@code false} otherwise
     */
    public boolean requiresAction() {
        return this == EXPIRED || this == SUSPENDED;
    }
}
