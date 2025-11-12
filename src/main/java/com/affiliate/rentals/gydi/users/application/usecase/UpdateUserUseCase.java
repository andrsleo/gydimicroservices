package com.affiliate.rentals.gydi.users.application.usecase;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.application.dto.UpdateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.RoleName;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Use case for updating an existing user.
 *
 * <p>This service handles the business logic for user updates,
 * including role management.</p>
 *
 * <p><b>SECURITY: IDOR Prevention</b> - Validates that users can only update their own data.</p>
 * <p><b>NOTE:</b> Changing roles should require ADMIN permission (not yet implemented).</p>
 *
 * @author GYDI Development Team
 */
@Service
public class UpdateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserDtoMapper mapper;
    private final OwnershipValidator ownershipValidator;

    public UpdateUserUseCase(
            UserRepositoryPort userRepository,
            UserDtoMapper mapper,
            OwnershipValidator ownershipValidator
    ) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Executes the update user use case.
     *
     * @param id the user ID to update
     * @param request the update user request data
     * @return the updated user as a UserResponse
     * @throws UserNotFoundException if the user is not found
     * @throws com.affiliate.rentals.gydi.shared.exception.ForbiddenException if user doesn't own the resource
     */
    @Transactional
    public UserResponse execute(Long id, UpdateUserRequest request) {
        // SECURITY: Validate ownership to prevent IDOR
        ownershipValidator.validateOwnership(id);

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
