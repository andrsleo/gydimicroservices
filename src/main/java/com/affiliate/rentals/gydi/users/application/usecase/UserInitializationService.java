package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.SubscriptionTransactionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for initializing newly registered users with Stripe Customer and FREE subscription.
 *
 * <p>
 * This service uses the same transaction as user registration (REQUIRED) with noRollbackFor
 * to ensure that failures in Stripe customer creation or subscription setup don't cause
 * the main user registration transaction to rollback.
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
public class UserInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(UserInitializationService.class);

    private final UserRepositoryPort userRepository;
    private final Optional<PaymentGatewayPort> paymentGateway;
    private final PlanRepositoryPort planRepository;
    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;

    public UserInitializationService(
            UserRepositoryPort userRepository,
            Optional<PaymentGatewayPort> paymentGateway,
            PlanRepositoryPort planRepository,
            UserSubscriptionRepositoryPort subscriptionRepository,
            SubscriptionTransactionRepositoryPort transactionRepository) {
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates a Stripe Customer for the newly registered user.
     *
     * <p>
     * Uses REQUIRED propagation with noRollbackFor to run in the same transaction
     * as user registration. If this fails, it's caught and logged, but the exception
     * won't cause rollback of the entire transaction.
     * </p>
     *
     * <p>
     * NOTE: This method only creates the Stripe customer and returns the ID.
     * It does NOT update the user in database to avoid optimistic locking conflicts.
     * The caller (CreateUserUseCase) is responsible for updating the user with the
     * Stripe Customer ID in the main transaction.
     * </p>
     *
     * @param user the user to create a Stripe Customer for
     * @return the Stripe Customer ID if successful, null otherwise
     */
    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = Exception.class)
    public String createStripeCustomerIfAvailable(User user) {
        if (paymentGateway.isEmpty()) {
            logger.debug("PaymentGateway not available - skipping Stripe Customer creation for user {}", user.email().address());
            return null;
        }

        try {
            logger.info("Creating Stripe Customer for user: {}", user.email().address());

            PaymentGatewayPort.CustomerResult customerResult = paymentGateway.get().createCustomer(
                user.email().address(),
                user.name(),
                String.format("userId:%d", user.id())
            );

            logger.info("Stripe Customer created successfully: {} for user {}", customerResult.id(), user.email().address());

            return customerResult.id();

        } catch (Exception e) {
            // Log error but don't fail - return null
            logger.error("Failed to create Stripe Customer for user {} - user will proceed without Stripe Customer ID. Error: {}",
                user.email().address(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a default FREE subscription for newly registered users.
     *
     * <p>
     * Uses REQUIRED propagation with noRollbackFor to run in the same transaction
     * as user registration. If this fails, it's caught and logged, but the exception
     * won't cause rollback of the entire transaction, allowing user registration to proceed.
     * </p>
     *
     * @param user the newly created user
     */
    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = Exception.class)
    public void createDefaultFreeSubscription(User user) {
        try {
            logger.info("Creating default FREE subscription for user: {}", user.email().address());

            // Find the FREE plan
            Optional<Plan> freePlan = planRepository.findByPlanCode("FREE");

            if (freePlan.isEmpty()) {
                logger.warn("FREE plan not found - skipping automatic subscription for user {}",
                        user.email().address());
                return;
            }

            Plan plan = freePlan.get();

            if (!plan.isActive()) {
                logger.warn("FREE plan is not active - skipping automatic subscription for user {}",
                        user.email().address());
                return;
            }

            // Check if user already has a subscription (shouldn't happen, but defensive)
            Optional<UserSubscription> existingSubscription = subscriptionRepository.findByUserId(user.id());
            if (existingSubscription.isPresent()) {
                logger.debug("User {} already has a subscription - skipping automatic FREE subscription",
                        user.email().address());
                return;
            }

            // Create FREE subscription
            LocalDateTime now = LocalDateTime.now();
            UserSubscription subscription = UserSubscription.builder()
                    .userId(user.id())
                    .planId(plan.id())
                    .status(SubscriptionStatus.ACTIVE)
                    .startedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(null) // FREE plan never expires
                    .paymentMethodId(null) // No payment method for FREE plan
                    .autoRenew(false) // FREE plan doesn't auto-renew
                    .nextBillingDate(null) // No billing for FREE plan
                    .build();

            UserSubscription savedSubscription = subscriptionRepository.save(subscription);

            // Create transaction record for audit trail
            SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                    .userSubscriptionId(savedSubscription.id())
                    .userId(user.id())
                    .paymentMethodId(null)
                    .transactionType(TransactionType.INITIAL_SUBSCRIPTION)
                    .transactionStatus(TransactionStatus.COMPLETED)
                    .toPlanId(plan.id())
                    .amount(plan.monthlyPrice()) // $0.00 for FREE plan
                    .currency(plan.currency())
                    .periodStart(now)
                    .periodEnd(null) // FREE plan has no end period
                    .processedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            transactionRepository.save(transaction);

            logger.info("Successfully created FREE subscription for user: {}", user.email().address());

        } catch (Exception e) {
            // Log error but don't fail registration
            logger.error("Failed to create FREE subscription for user {} - registration will proceed without subscription. Error: {}",
                    user.email().address(), e.getMessage(), e);
            // Don't rethrow - noRollbackFor prevents transaction rollback
        }
    }
}
