package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.application.dto.ChangePasswordRequest;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidPasswordException;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;

/**
 * Use case for changing an authenticated user's password.
 *
 * <p>
 * This service handles the business logic for password changes,
 * including verifying the current password before allowing the change.
 * </p>
 *
 * <p>
 * <b>SECURITY: IDOR Prevention</b> - Validates that users can only change their
 * own password by verifying ownership and current password.
 * </p>
 *
 * <p>
 * <b>Business Rules:</b>
 * </p>
 * <ul>
 * <li>User must provide correct current password</li>
 * <li>New password must meet strong password requirements (@StrongPassword)</li>
 * <li>New password must be different from current password</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class ChangePasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OwnershipValidator ownershipValidator;

    public ChangePasswordUseCase(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            OwnershipValidator ownershipValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Executes the change password use case.
     *
     * @param userId  the user ID whose password will be changed
     * @param request the change password request data
     * @throws UserNotFoundException                                          if the
     *                                                                        user is
     *                                                                        not found
     * @throws InvalidPasswordException                                       if the
     *                                                                        current
     *                                                                        password
     *                                                                        is
     *                                                                        incorrect
     * @throws com.affiliate.rentals.gydi.shared.exception.ForbiddenException if user
     *                                                                        doesn't
     *                                                                        own the
     *                                                                        resource
     * @throws IllegalArgumentException                                       if new
     *                                                                        password
     *                                                                        is same
     *                                                                        as current
     */
    @Transactional
    public void execute(Long userId, ChangePasswordRequest request) {
        // SECURITY: Validate ownership to prevent IDOR
        ownershipValidator.validateOwnership(userId);

        // Find user
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));

        // SECURITY: Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), existingUser.passwordHash())) {
            throw InvalidPasswordException.incorrectPassword();
        }

        // BUSINESS RULE: New password must be different from current
        if (passwordEncoder.matches(request.newPassword(), existingUser.passwordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Encode new password
        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        // Update user with new password hash
        User updatedUser = User.builder()
                .id(existingUser.id())
                .email(existingUser.email())
                .passwordHash(newPasswordHash)
                .name(existingUser.name())
                .phoneNumber(existingUser.phoneNumber())
                .roles(existingUser.roles())
                .activePlan(existingUser.activePlan())
                .capabilities(existingUser.capabilities())
                .accountVerified(existingUser.isAccountVerified())
                .createdAt(existingUser.createdAt())
                .build();

        userRepository.save(updatedUser);
    }
}
