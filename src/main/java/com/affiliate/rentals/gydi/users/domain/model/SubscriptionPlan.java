package com.affiliate.rentals.gydi.users.domain.model;

import java.math.BigDecimal;

/**
 * Enumeration of subscription plans in the GYDI platform.
 *
 * <p>This enum defines the available subscription tiers that determine a user's
 * capabilities, commission rates, and resource limits. Each plan provides different
 * levels of access to platform features.</p>
 *
 * <p>Plan details:</p>
 * <ul>
 *   <li>{@code FREE} - Entry-level plan with basic features (2% commission, 10 properties, 50 referrals)</li>
 *   <li>{@code PRO} - Mid-tier plan with enhanced features (5% commission, 50 properties, 200 referrals)</li>
 *   <li>{@code ELITE} - Premium plan with unlimited features (10% commission, unlimited properties and referrals)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SubscriptionPlan plan = SubscriptionPlan.PRO;
 * BigDecimal commission = plan.getCommissionRate();
 * int propertyLimit = plan.getPropertyPublishLimit();
 * boolean canUpgrade = plan.canUpgradeTo(SubscriptionPlan.ELITE);
 * }</pre>
 *
 * @author GYDI Development Team
 */
public enum SubscriptionPlan {
    /**
     * Free plan with basic features.
     * Commission: 2%, Property limit: 10, Referral limit: 50
     */
    FREE(
        BigDecimal.ZERO,
        new BigDecimal("0.02"),
        10,
        50
    ),

    /**
     * Pro plan with enhanced features.
     * Monthly price: $29.99, Commission: 5%, Property limit: 50, Referral limit: 200
     */
    PRO(
        new BigDecimal("29.99"),
        new BigDecimal("0.05"),
        50,
        200
    ),

    /**
     * Elite plan with premium features.
     * Monthly price: $99.99, Commission: 10%, Unlimited properties and referrals
     */
    ELITE(
        new BigDecimal("99.99"),
        new BigDecimal("0.10"),
        Integer.MAX_VALUE,
        Integer.MAX_VALUE
    );

    private final BigDecimal monthlyPrice;
    private final BigDecimal commissionRate;
    private final int propertyPublishLimit;
    private final int referralGenerateLimit;

    /**
     * Constructs a SubscriptionPlan with the specified parameters.
     *
     * @param monthlyPrice the monthly subscription price
     * @param commissionRate the commission rate for referrals (as decimal, e.g., 0.02 for 2%)
     * @param propertyPublishLimit maximum number of properties that can be published
     * @param referralGenerateLimit maximum number of referrals that can be generated
     */
    SubscriptionPlan(
        BigDecimal monthlyPrice,
        BigDecimal commissionRate,
        int propertyPublishLimit,
        int referralGenerateLimit
    ) {
        this.monthlyPrice = monthlyPrice;
        this.commissionRate = commissionRate;
        this.propertyPublishLimit = propertyPublishLimit;
        this.referralGenerateLimit = referralGenerateLimit;
    }

    /**
     * Returns the monthly price of this subscription plan.
     *
     * @return the monthly price, never {@code null}
     */
    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    /**
     * Returns the commission rate for this plan.
     *
     * @return the commission rate as a decimal (e.g., 0.05 for 5%), never {@code null}
     */
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    /**
     * Returns the maximum number of properties that can be published with this plan.
     *
     * @return the property publish limit
     */
    public int getPropertyPublishLimit() {
        return propertyPublishLimit;
    }

    /**
     * Returns the maximum number of referrals that can be generated with this plan.
     *
     * @return the referral generate limit
     */
    public int getReferralGenerateLimit() {
        return referralGenerateLimit;
    }

    /**
     * Checks if this plan is lower-tier than another plan.
     *
     * @param other the plan to compare against
     * @return {@code true} if this plan is lower than the other, {@code false} otherwise
     */
    public boolean isLowerThan(SubscriptionPlan other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Checks if this plan can be upgraded to another plan.
     * A plan can only be upgraded to a higher-tier plan.
     *
     * @param target the target plan to upgrade to
     * @return {@code true} if upgrade is allowed, {@code false} otherwise
     */
    public boolean canUpgradeTo(SubscriptionPlan target) {
        return this.isLowerThan(target);
    }

    /**
     * Checks if this plan has unlimited property publishing.
     *
     * @return {@code true} if property limit is unlimited, {@code false} otherwise
     */
    public boolean hasUnlimitedProperties() {
        return propertyPublishLimit == Integer.MAX_VALUE;
    }

    /**
     * Checks if this plan has unlimited referral generation.
     *
     * @return {@code true} if referral limit is unlimited, {@code false} otherwise
     */
    public boolean hasUnlimitedReferrals() {
        return referralGenerateLimit == Integer.MAX_VALUE;
    }

    /**
     * Returns a human-readable description of this plan's limits.
     *
     * @return a formatted string describing the plan limits
     */
    public String getLimitsDescription() {
        String properties = hasUnlimitedProperties() ? "Unlimited" : String.valueOf(propertyPublishLimit);
        String referrals = hasUnlimitedReferrals() ? "Unlimited" : String.valueOf(referralGenerateLimit);
        return "Properties: %s, Referrals: %s, Commission: %s%%"
            .formatted(properties, referrals, commissionRate.multiply(new BigDecimal("100")));
    }
}