package com.affiliate.rentals.gydi.users.domain.model;

import java.math.BigDecimal;

/**
 * Enumeration of subscription plans in the GYDI platform.
 *
 * <p>This enum defines the available subscription tiers that determine a user's
 * commission rates. Each plan provides different commission percentages for affiliates.</p>
 *
 * <p>⚠️ <strong>NOTA: LÍMITES DE RECURSOS DESACTIVADOS TEMPORALMENTE</strong></p>
 * <p>Los planes actualmente solo se diferencian por porcentaje de comisiones.
 * Todos los planes tienen límites ilimitados (Integer.MAX_VALUE) para propiedades y referidos.
 * Los límites de recursos NO se aplican por el momento.</p>
 *
 * <p>Plan details:</p>
 * <ul>
 *   <li>{@code FREE} - Entry-level plan (2% commission, unlimited properties and referrals)</li>
 *   <li>{@code PRO} - Mid-tier plan (5% commission, unlimited properties and referrals)</li>
 *   <li>{@code ELITE} - Premium plan (10% commission, unlimited properties and referrals)</li>
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
     * Commission: 2%, Unlimited properties and referrals (limits disabled)
     */
    FREE(
        BigDecimal.ZERO,
        new BigDecimal("0.02"),
        Integer.MAX_VALUE,  // Sin límite (desactivado)
        Integer.MAX_VALUE   // Sin límite (desactivado)
    ),

    /**
     * Pro plan with enhanced features.
     * Monthly price: $29.99, Commission: 5%, Unlimited properties and referrals (limits disabled)
     */
    PRO(
        new BigDecimal("29.99"),
        new BigDecimal("0.05"),
        Integer.MAX_VALUE,  // Sin límite (desactivado)
        Integer.MAX_VALUE   // Sin límite (desactivado)
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