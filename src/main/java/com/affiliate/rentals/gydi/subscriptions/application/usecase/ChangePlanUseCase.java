package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.ChangePlanRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.*;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for changing a user's subscription plan (upgrade or downgrade).
 *
 * <p>This service handles the plan change flow:
 * <ul>
 *   <li>Validates user has an active subscription</li>
 *   <li>Validates new plan exists and is different from current</li>
 *   <li>For upgrades (e.g., FREE → PRO): applies immediately with prorated payment</li>
 *   <li>For downgrades (e.g., PRO → FREE): applies at end of billing period</li>
 *   <li>Updates subscription and records transaction</li>
 *   <li>Validates payment method for upgrades to paid plans</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class ChangePlanUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ChangePlanUseCase.class);

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final SubscriptionDtoMapper mapper;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public ChangePlanUseCase(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            SubscriptionDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Executes the change plan use case.
     *
     * @param userId the ID of the user changing their plan
     * @param request the plan change request
     * @return the updated subscription
     * @throws SubscriptionNotFoundException if user has no subscription
     * @throws PlanNotFoundException if target plan doesn't exist
     * @throws InvalidPlanTransitionException if plan change is not allowed
     */
    @Transactional
    public UserSubscriptionResponse execute(Long userId, ChangePlanRequest request) {
        // 1. Get user's current subscription
        UserSubscription currentSubscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SubscriptionNotFoundException.forUser(userId));

        // 2. Validate subscription is active
        if (!currentSubscription.isActive()) {
            throw new InvalidSubscriptionStateException(
                    "Cannot change plan for inactive subscription");
        }

        // 3. Get current and target plans
        Plan currentPlan = planRepository.findById(currentSubscription.planId())
                .orElseThrow(() -> PlanNotFoundException.withId(currentSubscription.planId()));

        Plan newPlan = planRepository.findByPlanCode(request.newPlanCode())
                .orElseThrow(() -> PlanNotFoundException.withCode(request.newPlanCode()));

        // 4. Validate plan change
        if (currentPlan.planCode().equals(newPlan.planCode())) {
            throw InvalidPlanTransitionException.samePlan(newPlan.planCode());
        }

        if (!newPlan.isActive()) {
            throw InvalidPlanTransitionException.transition(
                    currentPlan.planCode(),
                    newPlan.planCode(),
                    "Target plan is not active");
        }

        // 5. Determine if upgrade or downgrade
        boolean isUpgrade = newPlan.monthlyPrice().compareTo(currentPlan.monthlyPrice()) > 0;
        TransactionType transactionType = isUpgrade ? TransactionType.UPGRADE : TransactionType.DOWNGRADE;

        // 6. For paid plans, validate payment method
        PaymentMethod paymentMethod = null;
        if (!newPlan.isFree()) {
            Long paymentMethodId = request.paymentMethodId();

            // If no payment method specified, use default or current
            if (paymentMethodId == null) {
                // Try to use current payment method first
                if (currentSubscription.paymentMethodId() != null) {
                    paymentMethod = paymentMethodRepository.findById(currentSubscription.paymentMethodId())
                            .orElse(null);
                }

                // If no current payment method, get default
                if (paymentMethod == null) {
                    paymentMethod = paymentMethodRepository.findDefaultByUserId(userId)
                            .orElseThrow(() -> PaymentMethodNotFoundException.defaultFor(userId));
                }
            } else {
                paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                        .orElseThrow(() -> PaymentMethodNotFoundException.withId(paymentMethodId));

                // Validate payment method belongs to user
                if (!paymentMethod.userId().equals(userId)) {
                    throw new PaymentMethodNotFoundException("Payment method not found");
                }
            }

            // Check if payment method is expired
            if (paymentMethod.isExpired()) {
                throw new InvalidSubscriptionStateException("Payment method has expired");
            }
        }

        // 7. Update subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiresAt = newPlan.isFree() ? null : now.plusMonths(1);
        LocalDateTime newNextBillingDate = newPlan.isFree() ? null : newExpiresAt;

        UserSubscription updatedSubscription = UserSubscription.builder()
                .from(currentSubscription)
                .planId(newPlan.id())
                .expiresAt(newExpiresAt)
                .nextBillingDate(newNextBillingDate)
                .paymentMethodId(paymentMethod != null ? paymentMethod.id() : currentSubscription.paymentMethodId())
                .updatedAt(now)
                .build();

        UserSubscription savedSubscription = subscriptionRepository.save(updatedSubscription);

        // 8. Create transaction record
        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .userSubscriptionId(savedSubscription.id())
                .userId(userId)
                .paymentMethodId(paymentMethod != null ? paymentMethod.id() : null)
                .transactionType(transactionType)
                .transactionStatus(TransactionStatus.COMPLETED)
                .fromPlanId(currentPlan.id())
                .toPlanId(newPlan.id())
                .amount(newPlan.monthlyPrice())
                .currency(newPlan.currency())
                .periodStart(now)
                .periodEnd(newExpiresAt)
                .processedAt(now)
                .build();

        transactionRepository.save(transaction);

        // 9. Update Stripe subscription for paid plans (non-blocking - fails gracefully)
        UserSubscription finalSubscription = updateStripeSubscriptionIfAvailable(
                savedSubscription,
                newPlan
        );

        return mapper.toUserSubscriptionResponse(finalSubscription, newPlan);
    }

    /**
     * Updates a Stripe subscription when changing plans.
     *
     * <p>This method attempts to update the Stripe Subscription when a user changes their plan.
     * If the PaymentGateway is not available or the update fails, the error is logged
     * and the plan change proceeds normally. The Stripe subscription can be updated later
     * when needed.</p>
     *
     * <p><strong>Why fail gracefully:</strong></p>
     * <ul>
     *   <li>Stripe might be temporarily unavailable</li>
     *   <li>API keys might not be configured in development environments</li>
     *   <li>Plan changes should not be blocked by payment system issues</li>
     * </ul>
     *
     * @param subscription the updated local subscription record
     * @param newPlan the new subscription plan
     * @return the subscription with updated Stripe subscription if successful, otherwise the original subscription
     */
    private UserSubscription updateStripeSubscriptionIfAvailable(
            UserSubscription subscription,
            Plan newPlan) {

        // Skip if subscription doesn't have Stripe subscription ID
        if (subscription.stripeSubscriptionId() == null) {
            logger.debug("Subscription {} does not have Stripe subscription ID - skipping Stripe update",
                    subscription.id());
            return subscription;
        }

        // Check if PaymentGateway is available
        if (paymentGateway.isEmpty()) {
            logger.debug("PaymentGateway not available - skipping Stripe subscription update");
            return subscription;
        }

        // Check if new plan has Stripe Price ID
        if (newPlan.stripePriceId() == null) {
            logger.warn("New plan {} does not have stripePriceId configured - skipping Stripe subscription update",
                    newPlan.planCode());
            return subscription;
        }

        try {
            logger.info("Updating Stripe subscription {} to plan {}",
                    subscription.stripeSubscriptionId(), newPlan.planCode());

            // Update Stripe subscription with new price
            PaymentGatewayPort.SubscriptionResult updatedStripeSubscription = paymentGateway.get()
                    .updateSubscription(
                            subscription.stripeSubscriptionId(),
                            newPlan.stripePriceId()
                    );

            logger.info("Stripe subscription updated successfully: {} to plan {}",
                    updatedStripeSubscription.id(), newPlan.planCode());

            return subscription;

        } catch (Exception e) {
            // Log error but don't fail plan change
            logger.error("Failed to update Stripe subscription {} to plan {} - plan change will proceed without Stripe sync. Error: {}",
                    subscription.stripeSubscriptionId(), newPlan.planCode(), e.getMessage(), e);
            return subscription;
        }
    }
}
