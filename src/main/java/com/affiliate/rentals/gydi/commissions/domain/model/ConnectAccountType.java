package com.affiliate.rentals.gydi.commissions.domain.model;

/**
 * Stripe Connect Account Types.
 * <p>
 * Different types based on region and requirements:
 * - STANDARD: US, CA, UK (full control, user manages everything)
 * - EXPRESS: EU countries (simplified onboarding, Stripe handles most)
 * - CUSTOM: Other countries (platform controls UI, more complex)
 * </p>
 */
public enum ConnectAccountType {
    /**
     * Standard accounts (US, CA, UK).
     * User has full control, manages everything via Stripe Dashboard.
     */
    STANDARD,

    /**
     * Express accounts (EU countries).
     * Simplified onboarding, Stripe handles most compliance.
     */
    EXPRESS,

    /**
     * Custom accounts (other countries).
     * Platform controls UI, more flexibility but more responsibility.
     */
    CUSTOM
}
