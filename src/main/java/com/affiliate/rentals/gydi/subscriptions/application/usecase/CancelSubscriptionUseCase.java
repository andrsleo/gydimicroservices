package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CancelSubscriptionRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.InvalidSubscriptionStateException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.SubscriptionNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use case for canceling a user's subscription.
 *
 * <p>This service handles the subscription cancellation flow:
 * <ul>
 *   <li>Validates user has an active subscription</li>
 *   <li>Supports immediate or end-of-period cancellation</li>
 *   <li>Updates subscription status and records reason</li>
 *   <li>Creates cancellation transaction for audit trail</li>
 *   <li>For paid plans: cancels in payment gateway (Stripe)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class CancelSubscriptionUseCase {

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final SubscriptionDtoMapper mapper;

    public CancelSubscriptionUseCase(
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
     * Executes the cancel subscription use case.
     *
     * @param userId the ID of the user canceling their subscription
     * @param request the cancellation request with reason and timing
     * @return the canceled subscription
     * @throws SubscriptionNotFoundException if user has no subscription
     * @throws InvalidSubscriptionStateException if subscription cannot be canceled
     */
    @Transactional
    public UserSubscriptionResponse execute(Long userId, CancelSubscriptionRequest request) {
        // 1. Get user's current subscription
        UserSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SubscriptionNotFoundException.forUser(userId));

        // 2. Validate subscription can be canceled
        if (subscription.status() == SubscriptionStatus.CANCELED) {
            throw InvalidSubscriptionStateException.cannotCancel("CANCELED");
        }

        // 3. Get plan details
        Plan plan = planRepository.findById(subscription.planId())
                .orElse(null);

        // 4. Cancel subscription
        LocalDateTime now = LocalDateTime.now();
        UserSubscription canceledSubscription;

        if (request.cancelImmediately()) {
            // Immediate cancellation: set status to CANCELED now
            canceledSubscription = subscription.withCancelation(request.reason());

            // For immediate cancellation, set expiration to now
            canceledSubscription = UserSubscription.builder()
                    .from(canceledSubscription)
                    .expiresAt(now)
                    .autoRenew(false)
                    .build();
        } else {
            // End-of-period cancellation: mark for cancellation but keep active until expiry
            canceledSubscription = UserSubscription.builder()
                    .from(subscription)
                    .autoRenew(false) // Disable auto-renewal
                    .canceledAt(now)
                    .cancelationReason(request.reason())
                    .updatedAt(now)
                    .build();
        }

        UserSubscription savedSubscription = subscriptionRepository.save(canceledSubscription);

        // 5. Create cancellation transaction
        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .userSubscriptionId(savedSubscription.id())
                .userId(userId)
                .paymentMethodId(null) // No payment for cancellations
                .transactionType(TransactionType.CANCELLATION)
                .transactionStatus(TransactionStatus.COMPLETED)
                .fromPlanId(plan != null ? plan.id() : null)
                .amount(BigDecimal.ZERO)
                .currency(plan != null ? plan.currency() : "USD")
                .periodStart(now)
                .periodEnd(savedSubscription.expiresAt())
                .processedAt(now)
                .build();

        transactionRepository.save(transaction);

        // TODO: For paid plans, cancel subscription in Stripe
        // This will be implemented when PaymentGatewayPort adapter is created
        // if (savedSubscription.stripeSubscriptionId() != null) {
        //     paymentGateway.cancelSubscription(
        //         savedSubscription.stripeSubscriptionId(),
        //         request.cancelImmediately()
        //     );
        // }

        return mapper.toUserSubscriptionResponse(savedSubscription, plan);
    }
}
