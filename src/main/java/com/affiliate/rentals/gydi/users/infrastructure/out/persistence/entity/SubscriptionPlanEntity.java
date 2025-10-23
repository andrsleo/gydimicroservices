package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

/**
 * Enum representing subscription plan types at the persistence layer.
 *
 * <p>This enum is used in JPA entities to persist subscription plan information.
 * It maps directly to the domain {@code SubscriptionPlan} enum but exists in the
 * infrastructure layer to maintain separation of concerns.</p>
 *
 * <p>Subscription plans:</p>
 * <ul>
 *   <li>{@code FREE} - Basic plan with 2% commission</li>
 *   <li>{@code PRO} - Enhanced plan with 5% commission</li>
 *   <li>{@code ELITE} - Premium plan with 10% commission</li>
 * </ul>
 *
 * @author GYDI Development Team
 * @see com.affiliate.rentals.gydi.users.domain.model.SubscriptionPlan
 */
public enum SubscriptionPlanEntity {
    /**
     * Free plan - 2% commission, basic features.
     */
    FREE,

    /**
     * Pro plan - 5% commission, enhanced features.
     */
    PRO,

    /**
     * Elite plan - 10% commission, premium features.
     */
    ELITE
}