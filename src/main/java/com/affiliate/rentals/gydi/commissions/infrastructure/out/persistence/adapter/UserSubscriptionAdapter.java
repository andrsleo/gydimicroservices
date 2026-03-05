package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.ports.PublishedPropertyQueryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Adapter to access user subscription data from subscriptions bounded context.
 * Anti-corruption layer.
 * <p>
 * Affiliate commission rate is property-based (not plan-based):
 * - 6% if the affiliate has at least one PUBLISHED property
 * - 4% otherwise (default)
 * The host commission rate remains plan-based.
 * </p>
 */
@Component
public class UserSubscriptionAdapter implements UserSubscriptionPort {

    /** Affiliate earns 6% when they have at least one published property. */
    private static final BigDecimal AFFILIATE_RATE_BOOSTED = new BigDecimal("0.06");

    /** Affiliate earns 4% by default (no published property). */
    private static final BigDecimal AFFILIATE_RATE_BASE = new BigDecimal("0.04");

    private final UserSubscriptionRepositoryPort userSubscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final PublishedPropertyQueryPort publishedPropertyQueryPort;

    public UserSubscriptionAdapter(
        UserSubscriptionRepositoryPort userSubscriptionRepository,
        PlanRepositoryPort planRepository,
        PublishedPropertyQueryPort publishedPropertyQueryPort
    ) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.planRepository = planRepository;
        this.publishedPropertyQueryPort = publishedPropertyQueryPort;
    }

    @Override
    public String getUserPlanCode(Long userId) {
        return getUserPlanData(userId).planCode();
    }

    @Override
    public BigDecimal getHostCommissionRate(Long userId) {
        return getUserPlanData(userId).hostCommissionRate();
    }

    /**
     * Returns the affiliate commission rate based on published property status:
     * - 6% if the affiliate has at least one PUBLISHED property
     * - 4% otherwise
     *
     * <p>Rate is captured as a snapshot at booking time (ReserveBookingUseCase).
     * Deactivating a property after the booking does not change the snapshotted rate.</p>
     */
    @Override
    public BigDecimal getAffiliateCommissionRate(Long userId) {
        boolean hasPublishedProperty = publishedPropertyQueryPort.hasPublishedProperty(userId);
        return hasPublishedProperty ? AFFILIATE_RATE_BOOSTED : AFFILIATE_RATE_BASE;
    }

    @Override
    public UserPlanData getUserPlanData(Long userId) {
        // Get user's active subscription
        UserSubscription subscription = userSubscriptionRepository.findByUserId(userId)
            .orElse(null);

        // If no active subscription, default to FREE plan
        Plan plan;
        if (subscription == null) {
            plan = planRepository.findByPlanCode("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE plan not found in database"));
        } else {
            plan = planRepository.findById(subscription.planId())
                .orElseThrow(() -> new IllegalStateException("Plan not found for ID: " + subscription.planId()));
        }

        return new UserPlanData(
            plan.planCode(),
            plan.hostCommissionRate(),
            plan.affiliateCommissionRate()
        );
    }
}
