package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.SubscribeToPlanRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.UserSubscriptionResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.exception.*;
import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.*;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
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
    private final UserRepositoryPort userRepository;
    private final SubscriptionDtoMapper mapper;
    private final Optional<PaymentGatewayPort> paymentGateway;

    public SubscribeToPlanUseCase(
            PlanRepositoryPort planRepository,
            UserSubscriptionRepositoryPort subscriptionRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            UserRepositoryPort userRepository,
            SubscriptionDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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

        // 2. Check if user already has active subscription
        subscriptionRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isActive()) {
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

        // 4. Create subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = plan.isFree() ? null : now.plusMonths(1);
        LocalDateTime nextBillingDate = plan.isFree() ? null : expiresAt;

        UserSubscription subscription = UserSubscription.builder()
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

        UserSubscription savedSubscription = subscriptionRepository.save(subscription);

        // 5. Create transaction record
        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .userSubscriptionId(savedSubscription.id())
                .userId(userId)
                .paymentMethodId(paymentMethod != null ? paymentMethod.id() : null)
                .transactionType(TransactionType.INITIAL_SUBSCRIPTION)
                .transactionStatus(TransactionStatus.COMPLETED)
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

        // 6. For paid plans, create Stripe subscription (non-blocking - fails
        // gracefully)
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
     * This method attempts to create a Stripe Subscription immediately during
     * subscription.
     * If the PaymentGateway is not available or the creation fails, the error is
     * logged
     * and the subscription proceeds normally. The Stripe subscription can be
     * created later
     * when needed.
     * </p>
     *
     * <p>
     * <strong>Why fail gracefully:</strong>
     * </p>
     * <ul>
     * <li>Stripe might be temporarily unavailable</li>
     * <li>API keys might not be configured in development environments</li>
     * <li>User subscription should not be blocked by payment system issues</li>
     * </ul>
     *
     * @param subscription          the local subscription record
     * @param plan                  the subscription plan
     * @param userId                the user ID
     * @param stripePaymentMethodId the Stripe payment method ID
     * @return the subscription with stripeSubscriptionId set if successful,
     *         otherwise the original subscription
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
            // Get user's Stripe Customer ID
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            if (user.stripeCustomerId() == null) {
                logger.error("❌ CRITICAL: User {} does not have Stripe Customer ID - cannot create subscription",
                        userId);
                logger.error(
                        "   → This should have been created during registration or lazy-created when adding payment method");
                return subscription;
            }

            logger.info("🚀 Creating Stripe subscription for user {} (customer: {}) on plan {} (price: {})",
                    userId, user.stripeCustomerId(), plan.planCode(), plan.stripePriceId());
            logger.info("   → Payment Method: {}", stripePaymentMethodId != null ? stripePaymentMethodId : "default");

            // Create Stripe subscription
            PaymentGatewayPort.SubscriptionResult stripeSubscription = paymentGateway.get().createSubscription(
                    user.stripeCustomerId(),
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

        } catch (Exception e) {
            // Log error with full stack trace
            logger.error("❌ CRITICAL ERROR: Failed to create Stripe subscription for user {} on plan {}",
                    userId, plan.planCode());
            logger.error("   → Error Type: {}", e.getClass().getSimpleName());
            logger.error("   → Error Message: {}", e.getMessage());
            logger.error("   → Full Stack Trace:", e);
            logger.error("   → Subscription will proceed WITHOUT Stripe integration!");
            return subscription;
        }
    }
}
