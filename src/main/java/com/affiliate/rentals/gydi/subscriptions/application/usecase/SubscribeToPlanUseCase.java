package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.SubscribeToPlanRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for subscribing a user to a subscription plan.
 *
 * <p>
 * This service handles the complete subscription flow:
 * <ul>
 * <li>Validates the plan exists and is active</li>
 * <li>Checks if user already has an active subscription</li>
 * <li>For FREE plan: creates subscription immediately</li>
 * <li>For paid plans: validates payment method and processes payment</li>
 * <li>Records transaction for audit trail</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class SubscribeToPlanUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeToPlanUseCase.class);

    private final PlanRepositoryPort planRepository;
    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final SubscriptionDtoMapper mapper;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public SubscribeToPlanUseCase(
            PlanRepositoryPort planRepository,
            UserSubscriptionRepositoryPort subscriptionRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            StripeConnectAccountRepositoryPort connectAccountRepository,
            SubscriptionDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.connectAccountRepository = connectAccountRepository;
        this.mapper = mapper;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Executes the subscribe to plan use case.
     *
     * @param userId  the ID of the user subscribing
     * @param request the subscription request containing plan code and payment
     *                details
     * @return the created subscription
     * @throws PlanNotFoundException             if plan doesn't exist
     * @throws InvalidSubscriptionStateException if user already has active
     *                                           subscription
     * @throws PaymentMethodNotFoundException    if payment method required but not
     *                                           found
     */
    @Transactional
    public UserSubscriptionResponse execute(Long userId, SubscribeToPlanRequest request) {
        // 1. Validate plan exists and is active
        Plan plan = planRepository.findByPlanCode(request.planCode())
                .orElseThrow(() -> PlanNotFoundException.withCode(request.planCode()));

        if (!plan.isActive()) {
            throw new InvalidSubscriptionStateException("Plan " + request.planCode() + " is not active");
        }

        // 2. Check if user already has subscription
        Optional<UserSubscription> existingSubscriptionOpt = subscriptionRepository.findByUserId(userId);

        // 2.1 If user has existing subscription on PAID plan and it's active → Block
        existingSubscriptionOpt.ifPresent(existing -> {
            Plan existingPlan = planRepository.findById(existing.planId()).orElse(null);

            // Block if user has active PAID plan (PRO/ELITE)
            if (existing.isActive() && existingPlan != null && !existingPlan.isFree()) {
                throw InvalidSubscriptionStateException.alreadySubscribed(request.planCode());
            }
        });

        // 3. For paid plans, validate payment method
        PaymentMethod paymentMethod = null;
        if (!plan.isFree()) {
            if (request.paymentMethodId() == null) {
                throw new PaymentMethodNotFoundException("Payment method is required for paid plans");
            }

            paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                    .orElseThrow(() -> PaymentMethodNotFoundException.withId(request.paymentMethodId()));

            // Validate payment method belongs to user
            if (!paymentMethod.userId().equals(userId)) {
                throw new PaymentMethodNotFoundException("Payment method not found");
            }

            // Check if payment method is expired
            if (paymentMethod.isExpired()) {
                throw new InvalidSubscriptionStateException("Payment method has expired");
            }
        }

        // 4. Create or update subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = plan.isFree() ? null : now.plusMonths(1);
        LocalDateTime nextBillingDate = plan.isFree() ? null : expiresAt;

        UserSubscription subscription;

        if (existingSubscriptionOpt.isPresent()) {
            // Update existing subscription (upgrade from FREE or renew after cancellation)
            UserSubscription existing = existingSubscriptionOpt.get();
            logger.info("Updating existing subscription {} for user {} to plan {}",
                    existing.id(), userId, plan.planCode());

            subscription = UserSubscription.builder()
                    .from(existing)  // Keep existing ID and other fields
                    .planId(plan.id())
                    .status(SubscriptionStatus.ACTIVE)
                    .startedAt(now)  // Reset start date for new plan
                    .updatedAt(now)
                    .expiresAt(expiresAt)
                    .paymentMethodId(paymentMethod != null ? paymentMethod.id() : null)
                    .autoRenew(request.autoRenew())
                    .nextBillingDate(nextBillingDate)
                    .canceledAt(null)  // Clear cancellation fields
                    .cancelationReason(null)
                    .build();
        } else {
            // Create new subscription
            logger.info("Creating new subscription for user {} on plan {}", userId, plan.planCode());

            subscription = UserSubscription.builder()
                    .userId(userId)
                    .planId(plan.id())
                    .status(SubscriptionStatus.ACTIVE)
                    .startedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(expiresAt)
                    .paymentMethodId(paymentMethod != null ? paymentMethod.id() : null)
                    .autoRenew(request.autoRenew())
                    .nextBillingDate(nextBillingDate)
                    .build();
        }

        UserSubscription savedSubscription = subscriptionRepository.save(subscription);

        // 5. Create transaction record
        // Determine transaction type: UPGRADE if existing subscription, INITIAL_SUBSCRIPTION if new
        TransactionType transactionType = existingSubscriptionOpt.isPresent()
                ? TransactionType.UPGRADE
                : TransactionType.INITIAL_SUBSCRIPTION;

        Long fromPlanId = existingSubscriptionOpt.map(UserSubscription::planId).orElse(null);

        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .userSubscriptionId(savedSubscription.id())
                .userId(userId)
                .paymentMethodId(paymentMethod != null ? paymentMethod.id() : null)
                .transactionType(transactionType)
                .transactionStatus(TransactionStatus.COMPLETED)
                .fromPlanId(fromPlanId)  // Previous plan if upgrading
                .toPlanId(plan.id())
                .amount(plan.monthlyPrice())
                .currency(plan.currency())
                .periodStart(now)
                .periodEnd(expiresAt)
                .processedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(transaction);

        // 6. For paid plans, create Stripe subscription (throws PaymentFailedException on any failure)
        UserSubscription finalSubscription = createStripeSubscriptionIfAvailable(
                savedSubscription,
                plan,
                userId,
                paymentMethod != null ? paymentMethod.gatewayToken() : null);

        return mapper.toUserSubscriptionResponse(finalSubscription, plan);
    }

    /**
     * Creates a Stripe subscription for paid plans.
     *
     * <p>
     * This method attaches the payment method to the Stripe customer, then creates
     * the Stripe Subscription. Any failure (payment declined, insufficient funds,
     * network error, or misconfiguration) throws a {@link PaymentFailedException},
     * causing the enclosing {@code @Transactional} to roll back the local DB record.
     * </p>
     *
     * @param subscription          the local subscription record (not yet committed)
     * @param plan                  the subscription plan
     * @param userId                the user ID
     * @param stripePaymentMethodId the Stripe payment method token (pm_xxx)
     * @return the subscription updated with the Stripe subscription ID
     * @throws PaymentFailedException if Stripe payment fails for any reason
     */
    private UserSubscription createStripeSubscriptionIfAvailable(
            UserSubscription subscription,
            Plan plan,
            Long userId,
            String stripePaymentMethodId) {

        // Skip for FREE plans
        if (plan.isFree()) {
            logger.info("✅ Skipping Stripe subscription creation for FREE plan");
            return subscription;
        }

        // Check if PaymentGateway is available
        if (paymentGateway.isEmpty()) {
            logger.error("❌ CRITICAL: PaymentGateway not available - skipping Stripe subscription creation");
            logger.error("   → Check if Stripe API keys are configured in application.properties");
            return subscription;
        }

        // Check if plan has Stripe Price ID
        if (plan.stripePriceId() == null) {
            logger.error(
                    "❌ CRITICAL: Plan {} does not have stripePriceId configured - skipping Stripe subscription creation",
                    plan.planCode());
            logger.error("   → Run UPDATE subscriptions.plans SET stripe_price_id = 'price_xxx' WHERE plan_code = '{}'",
                    plan.planCode());
            return subscription;
        }

        try {
            // Get user's platform customer ID from Connect account
            String platformCustomerId = connectAccountRepository
                    .findPlatformCustomerIdByUserId(userId).orElse(null);

            if (platformCustomerId == null) {
                logger.error("❌ CRITICAL: User {} does not have a platform customer ID in Connect account - cannot create subscription",
                        userId);
                logger.error(
                        "   → This should have been created during registration or lazy-created when adding payment method");
                return subscription;
            }

            logger.info("🚀 Creating Stripe subscription for user {} (customer: {}) on plan {} (price: {})",
                    userId, platformCustomerId, plan.planCode(), plan.stripePriceId());
            logger.info("   → Payment Method: {}", stripePaymentMethodId != null ? stripePaymentMethodId : "default");

            // Attach payment method to customer before creating subscription
            // Stripe requires the PM to be attached to the customer first
            if (stripePaymentMethodId != null) {
                logger.info("🔗 Attaching payment method {} to customer {}", stripePaymentMethodId,
                        platformCustomerId);
                paymentGateway.get().attachPaymentMethod(stripePaymentMethodId, platformCustomerId);
                logger.info("   → Payment method attached successfully");
            }

            // Create Stripe subscription
            PaymentGatewayPort.SubscriptionResult stripeSubscription = paymentGateway.get().createSubscription(
                    platformCustomerId,
                    plan.stripePriceId(),
                    stripePaymentMethodId);

            logger.info("✅ Stripe subscription created successfully!");
            logger.info("   → Subscription ID: {}", stripeSubscription.id());
            logger.info("   → Status: {}", stripeSubscription.status());
            logger.info("   → Period: {} to {}", stripeSubscription.currentPeriodStart(),
                    stripeSubscription.currentPeriodEnd());

            // Update subscription with Stripe subscription ID
            UserSubscription updatedSubscription = UserSubscription.builder()
                    .from(subscription)
                    .stripeSubscriptionId(stripeSubscription.id())
                    .build();

            // Save updated subscription with Stripe subscription ID
            return subscriptionRepository.save(updatedSubscription);

        } catch (PaymentFailedException e) {
            // Payment was declined — re-throw so @Transactional rolls back the DB record
            logger.error("❌ Payment declined for user {} on plan {}: {}", userId, plan.planCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // Any other Stripe/gateway error also fails the subscription
            logger.error("❌ Failed to create Stripe subscription for user {} on plan {}: {}",
                    userId, plan.planCode(), e.getMessage(), e);
            throw PaymentFailedException.forPlan(plan.planCode(), e.getMessage());
        }
    }
}
