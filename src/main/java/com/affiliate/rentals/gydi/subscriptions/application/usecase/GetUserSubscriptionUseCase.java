package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.SubscriptionNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.SubscriptionTransactionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for retrieving a user's current subscription.
 *
 * <p>This service returns the active subscription for a specific user,
 * including plan details. If the user has no subscription, a FREE plan
 * is automatically created as a fallback.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class GetUserSubscriptionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetUserSubscriptionUseCase.class);

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final SubscriptionDtoMapper mapper;

    public GetUserSubscriptionUseCase(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            SubscriptionDtoMapper mapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get user subscription use case.
     *
     * <p>If the user doesn't have a subscription (e.g., registered with paid plan
     * but didn't complete payment), a FREE subscription is automatically created.</p>
     *
     * @param userId the ID of the user
     * @return the user's subscription with plan details
     */
    @Transactional
    public UserSubscriptionResponse execute(Long userId) {
        Optional<UserSubscription> existingSubscription = subscriptionRepository.findByUserId(userId);

        if (existingSubscription.isPresent()) {
            UserSubscription subscription = existingSubscription.get();
            Plan plan = planRepository.findById(subscription.planId())
                    .orElse(null); // Should not happen, but handle gracefully

            return mapper.toUserSubscriptionResponse(subscription, plan);
        }

        // User has no subscription - create FREE plan as fallback
        logger.info("User {} has no subscription - creating FREE plan as fallback", userId);
        return createDefaultFreeSubscription(userId);
    }

    /**
     * Creates a default FREE subscription for users without a subscription.
     *
     * <p>This is a fallback mechanism for users who:</p>
     * <ul>
     * <li>Registered with a paid plan selected but didn't complete payment</li>
     * <li>Had their subscription cancelled/expired</li>
     * <li>Any other edge case where they don't have an active subscription</li>
     * </ul>
     *
     * @param userId the user ID
     * @return the created FREE subscription response
     */
    private UserSubscriptionResponse createDefaultFreeSubscription(Long userId) {
        // Find FREE plan
        Plan freePlan = planRepository.findByPlanCode("FREE")
                .orElseThrow(() -> new SubscriptionNotFoundException("FREE plan not found in system"));

        if (!freePlan.isActive()) {
            throw new SubscriptionNotFoundException("FREE plan is not active");
        }

        // Create FREE subscription
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .userId(userId)
                .planId(freePlan.id())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(null) // FREE plan never expires
                .paymentMethodId(null) // No payment method for FREE
                .autoRenew(false) // FREE doesn't auto-renew
                .nextBillingDate(null) // No billing for FREE
                .build();

        UserSubscription savedSubscription = subscriptionRepository.save(subscription);

        // Create transaction record for audit trail
        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .userSubscriptionId(savedSubscription.id())
                .userId(userId)
                .paymentMethodId(null)
                .transactionType(TransactionType.INITIAL_SUBSCRIPTION)
                .transactionStatus(TransactionStatus.COMPLETED)
                .toPlanId(freePlan.id())
                .amount(freePlan.monthlyPrice()) // $0.00 for FREE
                .currency(freePlan.currency())
                .periodStart(now)
                .periodEnd(null) // FREE has no end period
                .processedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(transaction);

        logger.info("✅ Successfully created fallback FREE subscription for user {}", userId);

        return mapper.toUserSubscriptionResponse(savedSubscription, freePlan);
    }
}
