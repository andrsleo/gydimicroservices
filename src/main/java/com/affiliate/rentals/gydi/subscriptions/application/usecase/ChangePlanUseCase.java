package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.ChangePlanRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.*;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.*;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for changing a user's subscription plan (upgrade or downgrade).
 *
 * <p>
 * This service handles the plan change flow:
 * <ul>
 * <li>Validates user has an active subscription</li>
 * <li>Validates new plan exists and is different from current</li>
 * <li>For upgrades (e.g., FREE → PRO): applies immediately with prorated
 * payment</li>
 * <li>For downgrades (e.g., PRO → FREE): applies at end of billing period</li>
 * <li>Updates subscription and records transaction</li>
 * <li>Validates payment method for upgrades to paid plans</li>
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
    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    public ChangePlanUseCase(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            SubscriptionDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway,
            StripeConnectAccountRepositoryPort connectAccountRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.paymentGateway = paymentGateway;
        this.connectAccountRepository = connectAccountRepository;
    }

    /**
     * Executes the change plan use case.
     *
     * @param userId  the ID of the user changing their plan
     * @param request the plan change request
     * @return the updated subscription
     * @throws SubscriptionNotFoundException  if user has no subscription
     * @throws PlanNotFoundException          if target plan doesn't exist
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

        // 7. Sync with Stripe and get updated subscription details
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultExpiresAt = newPlan.isFree() ? null : now.plusMonths(1);
        LocalDateTime newExpiresAt = defaultExpiresAt;
        String updatedStripeSubscriptionId = currentSubscription.stripeSubscriptionId();

        // Handle downgrade to FREE: cancel Stripe subscription
        if (newPlan.isFree() && currentSubscription.stripeSubscriptionId() != null) {
            logger.info("⬇️ Downgrading to FREE plan - cancelling Stripe subscription");
            boolean cancelled = cancelStripeSubscription(currentSubscription.stripeSubscriptionId());

            // NOTE: We keep the stripeSubscriptionId even after cancel-at-period-end
            // This allows us to reactivate if user upgrades before period ends
            // Stripe webhooks will eventually remove it when truly cancelled
            if (cancelled) {
                logger.info("✅ Stripe subscription set to cancel at period end");
                // Keep updatedStripeSubscriptionId as-is (don't set to null)
            } else {
                logger.warn("⚠️ Failed to cancel Stripe subscription - proceeding with local downgrade");
            }
        }
        // Handle paid plan changes: sync with Stripe
        else if (!newPlan.isFree()) {
            // Pass the resolved payment method token so Stripe uses the correct (new) card
            String resolvedPaymentMethodToken = paymentMethod != null ? paymentMethod.gatewayToken() : null;
            StripeUpdateResult stripeResult = syncWithStripe(currentSubscription, newPlan, resolvedPaymentMethodToken);
            if (stripeResult.periodEnd() != null) {
                newExpiresAt = stripeResult.periodEnd();
            }
            if (stripeResult.subscriptionId() != null) {
                updatedStripeSubscriptionId = stripeResult.subscriptionId();
            }
        }

        LocalDateTime newNextBillingDate = newPlan.isFree() ? null : newExpiresAt;

        // 8. Update Subscription Entity with ALL changes in a single save
        UserSubscription updatedSubscription = UserSubscription.builder()
                .from(currentSubscription)
                .planId(newPlan.id())
                .stripeSubscriptionId(updatedStripeSubscriptionId) // ✅ Update Stripe ID here
                .expiresAt(newExpiresAt)
                .nextBillingDate(newNextBillingDate)
                .paymentMethodId(paymentMethod != null ? paymentMethod.id() : currentSubscription.paymentMethodId())
                .updatedAt(now)
                .build();

        UserSubscription savedSubscription = subscriptionRepository.save(updatedSubscription);

        // 9. Create transaction record
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

        return mapper.toUserSubscriptionResponse(savedSubscription, newPlan);
    }

    /**
     * Result record for Stripe synchronization operations.
     * Contains both the updated period end date and the Stripe subscription ID.
     */
    private record StripeUpdateResult(LocalDateTime periodEnd, String subscriptionId) {
        static StripeUpdateResult empty() {
            return new StripeUpdateResult(null, null);
        }

        static StripeUpdateResult of(LocalDateTime periodEnd, String subscriptionId) {
            return new StripeUpdateResult(periodEnd, subscriptionId);
        }
    }

    /**
     * Synchronizes the subscription with Stripe.
     * Creates a new subscription, updates existing one, or reactivates a canceled one.
     *
     * <p>
     * <strong>Important:</strong> This method does NOT save to database.
     * It only communicates with Stripe and returns the result data.
     * The caller is responsible for persisting the changes.
     *
     * <p>
     * <strong>Reactivation Logic:</strong> If a subscription exists in Stripe and is
     * scheduled to cancel at period end (cancel_at_period_end=true), updating it with
     * a new price automatically reactivates it. This allows users to upgrade from FREE
     * back to a paid plan without creating duplicate subscriptions.
     *
     * @param subscription         the current local subscription
     * @param newPlan              the target plan
     * @param paymentMethodToken   the Stripe PM token resolved from the request (pm_xxx);
     *                             use this instead of subscription's stored PM to ensure
     *                             the correct (possibly new) card is used
     * @return StripeUpdateResult containing period end and subscription ID
     * @throws PaymentFailedException if Stripe payment fails for any reason
     */
    private StripeUpdateResult syncWithStripe(UserSubscription subscription, Plan newPlan,
            String paymentMethodToken) {

        // Check if PaymentGateway is available
        if (paymentGateway.isEmpty()) {
            logger.debug("PaymentGateway not available - skipping Stripe subscription sync");
            return StripeUpdateResult.empty();
        }

        // Check if new plan has Stripe Price ID
        if (newPlan.stripePriceId() == null) {
            logger.warn("New plan {} does not have stripePriceId configured - skipping Stripe sync",
                    newPlan.planCode());
            return StripeUpdateResult.empty();
        }

        try {
            // Get user's platform customer ID from Connect account
            String platformCustomerId = connectAccountRepository
                    .findPlatformCustomerIdByUserId(subscription.userId()).orElse(null);

            if (platformCustomerId == null) {
                logger.error("❌ User {} does not have a platform customer ID in Connect account - cannot sync with Stripe",
                        subscription.userId());
                return StripeUpdateResult.empty();
            }

            PaymentGatewayPort.SubscriptionResult stripeSubscription;

            // ========== CRITICAL DECISION POINT ==========
            // Check if we have a Stripe subscription ID in our database
            if (subscription.stripeSubscriptionId() != null) {
                // We have a subscription ID - check its current state in Stripe
                logger.info("🔍 Checking existing Stripe subscription: {}", subscription.stripeSubscriptionId());

                try {
                    // Get current subscription state from Stripe
                    PaymentGatewayPort.SubscriptionResult existingSubscription =
                            paymentGateway.get().getSubscription(subscription.stripeSubscriptionId());

                    logger.info("   → Status: {}", existingSubscription.status());

                    // Check subscription status
                    if ("active".equals(existingSubscription.status()) ||
                        "trialing".equals(existingSubscription.status()) ||
                        "past_due".equals(existingSubscription.status())) {
                        // Subscription is still active (may be scheduled to cancel) - UPDATE it
                        // Note: Updating a subscription that's cancel_at_period_end automatically reactivates it
                        logger.info("🔄 Updating/reactivating Stripe subscription {} to plan {}",
                                subscription.stripeSubscriptionId(), newPlan.planCode());

                        stripeSubscription = paymentGateway.get().updateSubscription(
                                subscription.stripeSubscriptionId(),
                                newPlan.stripePriceId());

                        logger.info("✅ Stripe subscription updated successfully!");
                        logger.info("   → Status: {}", stripeSubscription.status());
                        logger.info("   → Period end: {}", stripeSubscription.currentPeriodEnd());

                        // Return result (subscription ID unchanged)
                        return StripeUpdateResult.of(
                                stripeSubscription.currentPeriodEnd(),
                                subscription.stripeSubscriptionId());

                    } else {
                        // Subscription is canceled, incomplete, or unpaid - CREATE NEW
                        logger.warn("⚠️ Subscription {} has status '{}' - creating new subscription",
                                subscription.stripeSubscriptionId(), existingSubscription.status());
                        // Fall through to create new subscription below
                    }

                } catch (Exception e) {
                    // Failed to get subscription (maybe it was deleted in Stripe) - CREATE NEW
                    logger.warn("⚠️ Failed to retrieve Stripe subscription {}: {} - creating new",
                            subscription.stripeSubscriptionId(), e.getMessage());
                    // Fall through to create new subscription below
                }
            }

            // Create NEW subscription (either no ID exists, or existing one is invalid)
            logger.info("🚀 Creating NEW Stripe subscription");
            logger.info("   → Customer: {}, Plan: {}, Price: {}",
                    platformCustomerId, newPlan.planCode(), newPlan.stripePriceId());
            logger.info("   → Payment Method: {}", paymentMethodToken != null ? paymentMethodToken : "none");

            // Attach payment method to customer before creating subscription.
            // Always use the token passed in from execute() — it reflects the card the user
            // selected for THIS request, not whatever was stored from a previous attempt.
            if (paymentMethodToken != null) {
                logger.info("🔗 Attaching payment method {} to customer {}", paymentMethodToken,
                        platformCustomerId);
                paymentGateway.get().attachPaymentMethod(paymentMethodToken, platformCustomerId);
                logger.info("   → Payment method attached successfully");
            }

            // Create NEW subscription in Stripe
            stripeSubscription = paymentGateway.get().createSubscription(
                    platformCustomerId,
                    newPlan.stripePriceId(),
                    paymentMethodToken);

            logger.info("✅ Stripe subscription created successfully!");
            logger.info("   → Subscription ID: {}", stripeSubscription.id());
            logger.info("   → Status: {}", stripeSubscription.status());

            // Return result WITH new subscription ID
            return StripeUpdateResult.of(
                    stripeSubscription.currentPeriodEnd(),
                    stripeSubscription.id());

        } catch (PaymentFailedException e) {
            // Payment was declined — re-throw so @Transactional rolls back
            logger.error("❌ Payment declined for user {} changing to plan {}: {}",
                    subscription.userId(), newPlan.planCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // Any other Stripe/gateway error also fails the plan change
            logger.error("❌ Failed to sync Stripe subscription for user {} to plan {}: {}",
                    subscription.userId(), newPlan.planCode(), e.getMessage(), e);
            throw PaymentFailedException.forPlan(newPlan.planCode(), e.getMessage());
        }
    }

    /**
     * Cancels a Stripe subscription at period end.
     *
     * <p>
     * This is called when a user downgrades to the FREE plan.
     * We use cancel_at_period_end to allow the user to continue
     * accessing the paid features until their billing period ends,
     * and to allow reactivation if they upgrade before the period ends.
     * </p>
     *
     * @param stripeSubscriptionId the Stripe subscription ID to cancel
     * @return true if cancelled successfully, false otherwise
     */
    private boolean cancelStripeSubscription(String stripeSubscriptionId) {
        if (paymentGateway.isEmpty()) {
            logger.warn("PaymentGateway not available - cannot cancel Stripe subscription");
            return false;
        }

        try {
            logger.info("Cancelling Stripe subscription at period end: {}", stripeSubscriptionId);

            paymentGateway.get().cancelSubscription(
                    stripeSubscriptionId,
                    true // Cancel at period end (not immediately)
            );

            logger.info("Stripe subscription {} set to cancel at period end", stripeSubscriptionId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to cancel Stripe subscription {}: {}",
                    stripeSubscriptionId, e.getMessage(), e);
            return false;
        }
    }
}
