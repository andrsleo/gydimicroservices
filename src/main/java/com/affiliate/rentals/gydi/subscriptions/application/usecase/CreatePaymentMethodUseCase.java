package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.CreatePaymentMethodRequest;
import com.affiliate.rentals.gydi.subscriptions.application.dto.PaymentMethodResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Use case for creating a new payment method for a user.
 *
 * <p>
 * This service handles the payment method creation flow:
 * <ul>
 * <li>Receives tokenized payment method from Stripe</li>
 * <li>Stores payment method with only last 4 digits of card</li>
 * <li>If first payment method, automatically sets as default</li>
 * <li>If marked as default, removes default flag from other methods</li>
 * <li>NEVER stores full card numbers (PCI compliance)</li>
 * </ul>
 *
 * <p>
 * <b>SECURITY:</b> The frontend must tokenize payment details using
 * Stripe Elements before sending to backend. Raw card data should NEVER
 * reach the backend.
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
public class CreatePaymentMethodUseCase {

        private static final Logger logger = LoggerFactory.getLogger(CreatePaymentMethodUseCase.class);

        private final PaymentMethodRepositoryPort paymentMethodRepository;
        private final UserRepositoryPort userRepository;
        private final SubscriptionDtoMapper mapper;
        private final Optional<PaymentGatewayPort> paymentGateway;

        // Concurrency control for lazy customer creation
        private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

        public CreatePaymentMethodUseCase(
                        PaymentMethodRepositoryPort paymentMethodRepository,
                        UserRepositoryPort userRepository,
                        SubscriptionDtoMapper mapper,
                        Optional<PaymentGatewayPort> paymentGateway) {
                this.paymentMethodRepository = paymentMethodRepository;
                this.userRepository = userRepository;
                this.mapper = mapper;
                this.paymentGateway = paymentGateway;
        }

        /**
         * Executes the create payment method use case.
         *
         * @param userId  the ID of the user adding the payment method
         * @param request the payment method creation request with Stripe token
         * @return the created payment method
         */
        @Transactional
        public PaymentMethodResponse execute(Long userId, CreatePaymentMethodRequest request) {
                // 1. Check if user has any payment methods
                List<PaymentMethod> existingMethods = paymentMethodRepository.findActiveByUserId(userId);
                boolean isFirstPaymentMethod = existingMethods.isEmpty();

                // 2. If this should be default, remove default flag from others
                boolean shouldBeDefault = request.isDefault() || isFirstPaymentMethod;

                if (shouldBeDefault && !existingMethods.isEmpty()) {
                        existingMethods.forEach(method -> {
                                if (method.isDefault()) {
                                        PaymentMethod updatedMethod = method.makeDefault();
                                        updatedMethod = PaymentMethod.builder()
                                                        .from(updatedMethod)
                                                        .isDefault(false)
                                                        .build();
                                        paymentMethodRepository.save(updatedMethod);
                                }
                        });
                }

                // 3. Retrieve payment method details from Stripe and attach to customer
                PaymentMethodDetails paymentMethodDetails = retrieveAndAttachPaymentMethod(
                                userId,
                                request.stripePaymentMethodId());

                // 4. Create payment method with real or fallback data
                PaymentMethod paymentMethod = PaymentMethod.builder()
                                .userId(userId)
                                .methodType(PaymentMethodType.STRIPE)
                                .gatewayProvider("STRIPE")
                                .gatewayToken(request.stripePaymentMethodId())
                                .cardLastFour(paymentMethodDetails.cardLastFour())
                                .cardBrand(paymentMethodDetails.cardBrand())
                                .cardExpMonth(paymentMethodDetails.cardExpMonth())
                                .cardExpYear(paymentMethodDetails.cardExpYear())
                                .billingEmail(request.billingEmail() != null && !request.billingEmail().isBlank()
                                                ? request.billingEmail()
                                                : userRepository.findById(userId).map(u -> u.email().address())
                                                                .orElse(null))
                                .isDefault(shouldBeDefault)
                                .createdAt(LocalDateTime.now())
                                .status(PaymentMethodStatus.ACTIVE)
                                .deletedAt(null)
                                .updatedAt(LocalDateTime.now())
                                .build();

                PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);

                return mapper.toPaymentMethodResponse(savedPaymentMethod);
        }

        /**
         * Retrieves payment method details from Stripe and attaches it to the customer.
         *
         * <p>
         * This method attempts to retrieve payment method details from Stripe and
         * attach it to the user's Stripe Customer. If the user doesn't have a Stripe
         * Customer yet,
         * it creates one automatically (lazy creation). If Stripe is unavailable, it
         * returns fallback
         * data.
         * </p>
         *
         * @param userId                the user ID
         * @param stripePaymentMethodId the Stripe payment method ID
         * @return payment method details (real from Stripe or fallback data)
         */
        private PaymentMethodDetails retrieveAndAttachPaymentMethod(Long userId, String stripePaymentMethodId) {
                // Check if PaymentGateway is available
                if (paymentGateway.isEmpty()) {
                        logger.warn("PaymentGateway not available - using fallback payment method data");
                        return getFallbackPaymentMethodDetails();
                }

                try {
                        // Get payment method details from Stripe
                        logger.info("Retrieving payment method details from Stripe: {}", stripePaymentMethodId);
                        PaymentGatewayPort.PaymentMethodResult stripePaymentMethod = paymentGateway.get()
                                        .getPaymentMethod(stripePaymentMethodId);

                        // Get user
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                        // Lazy create Stripe Customer if needed (with race condition protection)
                        String stripeCustomerId = user.stripeCustomerId();
                        if (stripeCustomerId == null) {
                                logger.info("User {} does not have Stripe Customer ID - creating one now (lazy creation)",
                                                userId);
                                stripeCustomerId = createStripeCustomerForUserSafely(userId);

                                if (stripeCustomerId == null) {
                                        logger.error("Failed to create Stripe Customer for user {} - payment method will not be attached",
                                                        userId);
                                        return getFallbackPaymentMethodDetails();
                                }
                        }

                        // Attach payment method to customer in Stripe
                        logger.info("Attaching payment method {} to customer {}",
                                        stripePaymentMethodId, stripeCustomerId);

                        paymentGateway.get().attachPaymentMethod(
                                        stripePaymentMethodId,
                                        stripeCustomerId);

                        logger.info("Payment method attached successfully");

                        // Return real data from Stripe
                        return new PaymentMethodDetails(
                                        stripePaymentMethod.cardLastFour(),
                                        stripePaymentMethod.cardBrand(),
                                        stripePaymentMethod.cardExpMonth(),
                                        stripePaymentMethod.cardExpYear());

                } catch (Exception e) {
                        logger.error("Failed to retrieve/attach payment method from Stripe: {} - using fallback data. Error: {}",
                                        stripePaymentMethodId, e.getMessage(), e);
                        return getFallbackPaymentMethodDetails();
                }
        }

        /**
         * Thread-safe wrapper for creating a Stripe Customer using double-checked
         * locking.
         * 
         * <p>
         * This method prevents race conditions when multiple concurrent requests
         * attempt to create a customer for the same user. It uses per-user locking
         * to ensure only one customer is created in Stripe.
         * </p>
         * 
         * @param userId the ID of the user
         * @return the Stripe Customer ID if successful, null if failed
         */
        private String createStripeCustomerForUserSafely(Long userId) {
                // Get or create a lock for this specific user
                Lock userLock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

                userLock.lock();
                try {
                        // Double-check: another thread might have created the customer while we waited
                        User freshUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                        if (freshUser.stripeCustomerId() != null) {
                                logger.info("Stripe Customer ID was created by another thread for user {}: {}",
                                                userId, freshUser.stripeCustomerId());
                                return freshUser.stripeCustomerId();
                        }

                        // Still null after double-check, proceed with creation
                        return createStripeCustomerForUser(freshUser);

                } finally {
                        userLock.unlock();
                        // Clean up lock after use to prevent memory leak
                        userLocks.remove(userId);
                }
        }

        /**
         * Creates a Stripe Customer for a user who doesn't have one yet.
         * 
         * <p>
         * <strong>IMPORTANT:</strong> This method should only be called from
         * {@link #createStripeCustomerForUserSafely(Long)} to ensure thread safety.
         * </p>
         * 
         * <p>
         * This method creates the customer in Stripe and saves the ID to the local
         * database.
         * The operation is idempotent - if Stripe reports the customer already exists
         * (based on
         * email or metadata), we retrieve and use the existing customer ID.
         * </p>
         * 
         * @param user the user to create a Stripe Customer for
         * @return the Stripe Customer ID if successful, null if failed
         */
        private String createStripeCustomerForUser(User user) {
                try {
                        logger.info("Creating Stripe Customer for user {} (lazy creation)", user.email().address());

                        PaymentGatewayPort.CustomerResult customerResult = paymentGateway.get().createCustomer(
                                        user.email().address(),
                                        user.name(),
                                        String.format("userId:%d", user.id()));

                        logger.info("Stripe Customer created successfully: {} for user {}",
                                        customerResult.id(), user.email().address());

                        // Update user with Stripe Customer ID
                        User userWithStripeId = User.builder()
                                        .id(user.id())
                                        .name(user.name())
                                        .email(user.email())
                                        .passwordHash(user.passwordHash())
                                        .phoneNumber(user.phoneNumber())
                                        .roles(user.roles())
                                        .activePlan(user.activePlan())
                                        .capabilities(user.capabilities())
                                        .accountVerified(user.isAccountVerified())
                                        .stripeCustomerId(customerResult.id())
                                        .createdAt(user.createdAt())
                                        .build();

                        // Save updated user with Stripe Customer ID
                        userRepository.save(userWithStripeId);

                        return customerResult.id();

                } catch (Exception e) {
                        logger.error("Failed to create Stripe Customer for user {} during lazy creation. Error: {}",
                                        user.email().address(), e.getMessage(), e);
                        return null;
                }
        }

        /**
         * Returns fallback payment method details when Stripe is unavailable.
         */
        private PaymentMethodDetails getFallbackPaymentMethodDetails() {
                return new PaymentMethodDetails(
                                "****", // Unknown last 4
                                "Unknown", // Unknown brand
                                12, // Default exp month
                                2099 // Far future exp year
                );
        }

        /**
         * Record for holding payment method details.
         */
        private record PaymentMethodDetails(
                        String cardLastFour,
                        String cardBrand,
                        Integer cardExpMonth,
                        Integer cardExpYear) {
        }
}
