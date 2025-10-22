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
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
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
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper mapper;

    public CreateUserUseCase(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder, UserDtoMapper mapper) {
        this.userRepository = userRepository;
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
                : Set.of(Role.guest());

        User user = User.builder()
                .email(email)
                .passwordHash(encodedPassword)
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return mapper.toResponse(savedUser);
    }
}
