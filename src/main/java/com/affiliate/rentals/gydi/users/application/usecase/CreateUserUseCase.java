package com.affiliate.rentals.gydi.users.application.usecase;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Use case for creating a new user.
 *
 * <p>
 * This service handles the business logic for user registration, including
 * password encoding, email validation, role assignment, and user profile
 * creation.
 * </p>
 *
 * <p>
 * Additional initialization tasks (Stripe Customer creation and FREE
 * subscription setup)
 * are delegated to {@link UserInitializationService} which handles them in
 * separate
 * transactions (REQUIRES_NEW) to prevent rollback of user registration if they
 * fail.
 * </p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
public class CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileRepositoryPort userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper mapper;
    private final UserInitializationService initializationService;

    public CreateUserUseCase(
            UserRepositoryPort userRepository,
            UserProfileRepositoryPort userProfileRepository,
            PasswordEncoder passwordEncoder,
            UserDtoMapper mapper,
            UserInitializationService initializationService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.initializationService = initializationService;
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

        // Create Stripe Customer in separate transaction (non-blocking - fails
        // gracefully)
        // Returns the Stripe Customer ID if successful, null otherwise
        String stripeCustomerId = initializationService.createStripeCustomerIfAvailable(savedUser);

        // Update user with Stripe Customer ID if it was created successfully
        User userWithStripeCustomer = savedUser;
        if (stripeCustomerId != null) {
            userWithStripeCustomer = User.builder()
                    .id(savedUser.id())
                    .name(savedUser.name())
                    .email(savedUser.email())
                    .passwordHash(savedUser.passwordHash())
                    .phoneNumber(savedUser.phoneNumber())
                    .roles(savedUser.roles())
                    .activePlan(savedUser.activePlan())
                    .capabilities(savedUser.capabilities())
                    .accountVerified(savedUser.isAccountVerified())
                    .stripeCustomerId(stripeCustomerId)
                    .createdAt(savedUser.createdAt())
                    .build();

            // Save user with Stripe Customer ID in the main transaction
            userWithStripeCustomer = userRepository.save(userWithStripeCustomer);
        }

        // Create FREE subscription only if:
        // 1. No plan was selected during registration (selectedPlanCode is null), OR
        // 2. User explicitly selected the FREE plan
        // For paid plans (PRO, ELITE), subscription will be created after payment
        boolean shouldCreateFreeSubscription = request.selectedPlanCode() == null
                || "FREE".equalsIgnoreCase(request.selectedPlanCode());

        if (shouldCreateFreeSubscription) {
            log.info("Creating FREE subscription for user {} (selectedPlanCode: {})",
                    userWithStripeCustomer.email().address(), request.selectedPlanCode());
            initializationService.createDefaultFreeSubscription(userWithStripeCustomer);
        } else {
            log.info("Skipping FREE subscription for user {} - paid plan selected: {}",
                    userWithStripeCustomer.email().address(), request.selectedPlanCode());
        }

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
}
