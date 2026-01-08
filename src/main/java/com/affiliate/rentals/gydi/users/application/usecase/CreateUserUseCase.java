package com.affiliate.rentals.gydi.users.application.usecase;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.SubscriptionTransactionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import com.affiliate.rentals.gydi.users.application.dto.CreateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserAlreadyExistsException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.RoleName;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Use case for creating a new user.
 *
 * <p>
 * This service handles the business logic for user registration, including
 * password encoding, email validation, role assignment, and Stripe Customer creation.
 * </p>
 *
 * <p>
 * The Stripe Customer is created asynchronously during registration for better performance.
 * If Stripe is unavailable or the customer creation fails, the registration proceeds normally
 * and the customer can be created later when the user attempts to use payment features.
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
public class CreateUserUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateUserUseCase.class);

    private final UserRepositoryPort userRepository;
    private final UserProfileRepositoryPort userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper mapper;
    private final Optional<PaymentGatewayPort> paymentGateway;
    private final PlanRepositoryPort planRepository;
    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;

    public CreateUserUseCase(
            UserRepositoryPort userRepository,
            UserProfileRepositoryPort userProfileRepository,
            PasswordEncoder passwordEncoder,
            UserDtoMapper mapper,
            Optional<PaymentGatewayPort> paymentGateway,
            PlanRepositoryPort planRepository,
            UserSubscriptionRepositoryPort subscriptionRepository,
            SubscriptionTransactionRepositoryPort transactionRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.paymentGateway = paymentGateway;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Executes the create user use case.
     *
     * @param request the create user request data
     * @return the created user as a UserResponse
     * @throws UserAlreadyExistsException if the email already exists
     */
    @Transactional
    public UserResponse execute(CreateUserRequest request) {
        Email email = Email.of(request.email());

        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException.withEmail(request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Set<Role> roles = request.roleNames() != null && !request.roleNames().isEmpty()
                ? request.roleNames().stream()
                        .map(RoleName::fromValue)
                        .map(roleName -> new Role(null, roleName))
                        .collect(Collectors.toSet())
                : Set.of(Role.user());

        // Build full name from firstName and lastName for backwards compatibility
        String fullName = request.name() != null
                ? request.name()
                : buildFullName(request.firstName(), request.lastName());

        User user = User.builder()
                .email(email)
                .passwordHash(encodedPassword)
                .name(fullName) // Still save to users.name for backwards compatibility
                .phoneNumber(request.phoneNumber())
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        // Create UserProfile with names (this is now the source of truth for names)
        createDefaultUserProfile(savedUser, request.firstName(), request.lastName(), request.phoneNumber());

        // Create Stripe Customer (non-blocking - fails gracefully)
        User userWithStripeCustomer = createStripeCustomerIfAvailable(savedUser);

        // Create FREE subscription for new users (non-blocking - fails gracefully)
        createDefaultFreeSubscription(userWithStripeCustomer);

        return mapper.toResponse(userWithStripeCustomer);
    }

    /**
     * Builds a full name from first and last names.
     *
     * @param firstName the first name
     * @param lastName  the last name (optional)
     * @return the full name
     */
    private String buildFullName(String firstName, String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    /**
     * Creates a default UserProfile for a newly registered user.
     *
     * <p>
     * This method is called automatically during user registration to ensure
     * every user has an associated profile with sensible defaults. Names are saved
     * in the profile as the source of truth.
     * </p>
     *
     * @param user        the newly created user
     * @param firstName   the user's first name
     * @param lastName    the user's last name (optional)
     * @param phoneNumber the user's phone number (optional)
     */
    private void createDefaultUserProfile(User user, String firstName, String lastName, String phoneNumber) {
        UserProfile defaultProfile = UserProfile.builder()
                .userId(user.id())
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .preferredLanguage("en")
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .build();

        userProfileRepository.save(defaultProfile);
    }

    /**
     * Creates a default FREE subscription for newly registered users.
     *
     * <p>
     * This method automatically assigns the FREE (BASIC) plan to new users during registration.
     * If the plan repository is not available or the creation fails, the error is logged
     * and the registration proceeds normally. The subscription can be created later.
     * </p>
     *
     * <p><strong>Why fail gracefully:</strong></p>
     * <ul>
     *   <li>Subscription system might be temporarily unavailable</li>
     *   <li>FREE plan might not be configured in development environments</li>
     *   <li>User registration should not be blocked by subscription system issues</li>
     * </ul>
     *
     * @param user the newly created user
     */
    private void createDefaultFreeSubscription(User user) {
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
                    .build();

            transactionRepository.save(transaction);

            logger.info("Successfully created FREE subscription for user: {}", user.email().address());

        } catch (Exception e) {
            // Log error but don't fail registration
            logger.error("Failed to create FREE subscription for user {} - registration will proceed without subscription. Error: {}",
                    user.email().address(), e.getMessage(), e);
        }
    }

    /**
     * Creates a Stripe Customer for the newly registered user.
     *
     * <p>
     * This method attempts to create a Stripe Customer immediately during user registration.
     * If the PaymentGateway is not available or the creation fails, the error is logged
     * and the registration proceeds normally. The customer can be created later when the
     * user attempts to use payment features.
     * </p>
     *
     * <p><strong>Why fail gracefully:</strong></p>
     * <ul>
     *   <li>Stripe might be temporarily unavailable</li>
     *   <li>API keys might not be configured in development environments</li>
     *   <li>User registration should not be blocked by payment system issues</li>
     * </ul>
     *
     * @param user the user to create a Stripe Customer for
     * @return the user with stripeCustomerId set if successful, otherwise the original user
     */
    private User createStripeCustomerIfAvailable(User user) {
        if (paymentGateway.isEmpty()) {
            logger.debug("PaymentGateway not available - skipping Stripe Customer creation for user {}", user.email().address());
            return user;
        }

        try {
            logger.info("Creating Stripe Customer for user: {}", user.email().address());

            PaymentGatewayPort.CustomerResult customerResult = paymentGateway.get().createCustomer(
                user.email().address(),
                user.name(),
                String.format("userId:%d", user.id())
            );

            logger.info("Stripe Customer created successfully: {} for user {}", customerResult.id(), user.email().address());

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
            return userRepository.save(userWithStripeId);

        } catch (Exception e) {
            // Log error but don't fail registration
            logger.error("Failed to create Stripe Customer for user {} - registration will proceed without Stripe Customer ID. Error: {}",
                user.email().address(), e.getMessage(), e);
            return user;
        }
    }
}
