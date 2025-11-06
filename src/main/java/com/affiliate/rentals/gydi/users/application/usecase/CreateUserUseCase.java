package com.affiliate.rentals.gydi.users.application.usecase;

import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * Use case for creating a new user.
 *
 * <p>This service handles the business logic for user registration, including
 * password encoding, email validation, and role assignment.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserProfileRepositoryPort userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper mapper;

    public CreateUserUseCase(
            UserRepositoryPort userRepository,
            UserProfileRepositoryPort userProfileRepository,
            PasswordEncoder passwordEncoder,
            UserDtoMapper mapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
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
                .name(fullName)  // Still save to users.name for backwards compatibility
                .phoneNumber(request.phoneNumber())
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        // Create UserProfile with names (this is now the source of truth for names)
        createDefaultUserProfile(savedUser, request.firstName(), request.lastName());

        return mapper.toResponse(savedUser);
    }

    /**
     * Builds a full name from first and last names.
     *
     * @param firstName the first name
     * @param lastName the last name (optional)
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
     * <p>This method is called automatically during user registration to ensure
     * every user has an associated profile with sensible defaults. Names are saved
     * in the profile as the source of truth.</p>
     *
     * @param user the newly created user
     * @param firstName the user's first name
     * @param lastName the user's last name (optional)
     */
    private void createDefaultUserProfile(User user, String firstName, String lastName) {
        UserProfile defaultProfile = UserProfile.builder()
                .userId(user.id())
                .firstName(firstName)
                .lastName(lastName)
                .preferredLanguage("en")
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .build();

        userProfileRepository.save(defaultProfile);
    }
}
