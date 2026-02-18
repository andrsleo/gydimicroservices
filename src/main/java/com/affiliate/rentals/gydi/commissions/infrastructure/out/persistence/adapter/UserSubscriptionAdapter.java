package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

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
 */
@Component
public class UserSubscriptionAdapter implements UserSubscriptionPort {

    private final UserSubscriptionRepositoryPort userSubscriptionRepository;
    private final PlanRepositoryPort planRepository;

    public UserSubscriptionAdapter(
        UserSubscriptionRepositoryPort userSubscriptionRepository,
        PlanRepositoryPort planRepository
    ) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.planRepository = planRepository;
    }

    @Override
    public String getUserPlanCode(Long userId) {
        return getUserPlanData(userId).planCode();
    }

    @Override
    public BigDecimal getHostCommissionRate(Long userId) {
        return getUserPlanData(userId).hostCommissionRate();
    }

    @Override
    public BigDecimal getAffiliateCommissionRate(Long userId) {
        return getUserPlanData(userId).affiliateCommissionRate();
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
