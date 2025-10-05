package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UpdateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.RoleName;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use case for updating an existing user.
 *
 * <p>This service handles the business logic for user updates,
 * including role management.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    public UpdateUserUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the update user use case.
     *
     * @param id the user ID to update
     * @param request the update user request data
     * @return the updated user as a UserResponse
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional
    public UserResponse execute(Long id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.withId(id));

        Set<Role> roles = request.roleNames() != null && !request.roleNames().isEmpty()
                ? request.roleNames().stream()
                .map(RoleName::fromValue)
                .map(roleName -> new Role(null, roleName))
                .collect(Collectors.toSet())
                : existingUser.roles();

        User updatedUser = User.builder()
                .id(existingUser.id())
                .email(existingUser.email())
                .passwordHash(existingUser.passwordHash())
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .roles(roles)
                .createdAt(existingUser.createdAt())
                .build();

        User savedUser = userRepository.save(updatedUser);
        return mapper.toResponse(savedUser);
    }
}
