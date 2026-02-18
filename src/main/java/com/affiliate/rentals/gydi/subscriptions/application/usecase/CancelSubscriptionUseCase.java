package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CancelSubscriptionRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.InvalidSubscriptionStateException;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.SubscriptionNotFoundException;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort.SubscriptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

    private static final Logger log = LoggerFactory.getLogger(CancelSubscriptionUseCase.class);

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final SubscriptionDtoMapper mapper;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public CancelSubscriptionUseCase(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            SubscriptionDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.paymentGateway = paymentGateway;
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
            // Immediate cancellation: downgrade to FREE immediately
            Plan freePlan = planRepository.findByPlanCode("FREE")
                    .orElseThrow(() -> new IllegalStateException("FREE plan not found in database"));

            log.info("User {} canceling subscription immediately. Downgrading from plan {} to FREE (id: {})",
                    userId, subscription.planId(), freePlan.id());

            canceledSubscription = subscription.withCancelation(request.reason());

            // Downgrade to FREE plan immediately
            canceledSubscription = UserSubscription.builder()
                    .from(canceledSubscription)
                    .planId(freePlan.id())       // ✅ Downgrade to FREE
                    .expiresAt(null)             // FREE plan doesn't expire
                    .autoRenew(false)
                    .status(SubscriptionStatus.ACTIVE) // FREE is active, not canceled
                    .canceledAt(now)             // Record when they canceled
                    .cancelationReason(request.reason())
                    .build();

            log.info("User {} downgraded to FREE plan immediately", userId);
        } else {
            // End-of-period cancellation: mark for cancellation but keep active until expiry
            // Scheduler will downgrade to FREE when subscription expires
            log.info("User {} scheduling cancellation at end of period (expires: {})",
                    userId, subscription.expiresAt());

            canceledSubscription = UserSubscription.builder()
                    .from(subscription)
                    .autoRenew(false) // Disable auto-renewal
                    .canceledAt(now)
                    .cancelationReason(request.reason())
                    .updatedAt(now)
                    .build();

            log.info("User {} subscription will remain active until {}, then downgrade to FREE",
                    userId, subscription.expiresAt());
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

        // 6. Cancel subscription in Stripe (for paid plans)
        if (savedSubscription.stripeSubscriptionId() != null && paymentGateway.isPresent()) {
            try {
                SubscriptionResult result = paymentGateway.get().cancelSubscription(
                    savedSubscription.stripeSubscriptionId(),
                    request.cancelImmediately()
                );
                log.info("Stripe subscription {} canceled successfully. Status: {}",
                         savedSubscription.stripeSubscriptionId(), result.status());
            } catch (Exception e) {
                log.error("Failed to cancel Stripe subscription {}: {}",
                          savedSubscription.stripeSubscriptionId(), e.getMessage());
                // Continue anyway - local cancellation is already recorded
                // The user's subscription is canceled in our system
            }
        } else {
            log.info("Skipping Stripe cancellation - subscription has no Stripe ID or gateway not available");
        }

        return mapper.toUserSubscriptionResponse(savedSubscription, plan);
    }
}
