package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.SubscriptionNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a user's current subscription.
 *
 * <p>This service returns the active subscription for a specific user,
 * including plan details.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class GetUserSubscriptionUseCase {

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final SubscriptionDtoMapper mapper;

    public GetUserSubscriptionUseCase(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            SubscriptionDtoMapper mapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get user subscription use case.
     *
     * @param userId the ID of the user
     * @return the user's subscription with plan details
     * @throws SubscriptionNotFoundException if user has no subscription
     */
    @Transactional(readOnly = true)
    public UserSubscriptionResponse execute(Long userId) {
        UserSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SubscriptionNotFoundException.forUser(userId));

        Plan plan = planRepository.findById(subscription.planId())
                .orElse(null); // Should not happen, but handle gracefully

        return mapper.toUserSubscriptionResponse(subscription, plan);
    }
}
