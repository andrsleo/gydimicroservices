package com.affiliate.rentals.gydi.commissions.domain.ports;

import java.math.BigDecimal;

/**
 * Port interface for accessing user subscription data.
 * <p>
 * Anti-corruption layer between commissions and subscriptions bounded contexts.
 * Implemented by infrastructure adapter that calls subscriptions bounded context.
 * </p>
 */
public interface UserSubscriptionPort {

    /**
     * Gets user's current subscription plan code (FREE, PRO, ELITE).
     *
     * @param userId user ID
     * @return plan code or "FREE" if no active subscription
     */
    String getUserPlanCode(Long userId);

    /**
     * Gets host commission rate for user's current plan.
     *
     * @param userId user ID
     * @return commission rate (0.25 for FREE, 0.20 for PRO, 0.15 for ELITE)
     */
    BigDecimal getHostCommissionRate(Long userId);

    /**
     * Gets affiliate commission rate for user's current plan.
     *
     * @param userId user ID
     * @return commission rate (0.02 for FREE, 0.05 for PRO, 0.10 for ELITE)
     */
    BigDecimal getAffiliateCommissionRate(Long userId);

    /**
     * User subscription data record.
     */
    record UserPlanData(
        String planCode,
        BigDecimal hostCommissionRate,
        BigDecimal affiliateCommissionRate
    ) {}

    /**
     * Gets complete plan data for user.
     *
     * @param userId user ID
     * @return UserPlanData with plan code and rates
     */
    UserPlanData getUserPlanData(Long userId);
}
