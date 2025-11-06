package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.PasswordResetResponse;
import com.affiliate.rentals.gydi.users.application.dto.ResetPasswordRequest;
import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;
import com.affiliate.rentals.gydi.users.domain.ports.PasswordResetTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for resetting a user's password using a valid reset token.
 *
 * <p>This service handles the complete password reset process, including:</p>
 * <ul>
 *   <li>Validating the reset token</li>
 *   <li>Hashing the new password</li>
 *   <li>Updating the user's password</li>
 *   <li>Marking the token as used</li>
 *   <li>Invalidating all other tokens for the user</li>
 *   <li>Sending confirmation email</li>
 * </ul>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Password is validated at DTO level with strong requirements</li>
 *   <li>Token can only be used once</li>
 *   <li>All user sessions should be invalidated (handled by frontend/auth service)</li>
 *   <li>User receives confirmation email for security awareness</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class ResetPasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResetPasswordUseCase.class);

    private final UserRepositoryPort userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServicePort emailService;

    public ResetPasswordUseCase(
            UserRepositoryPort userRepository,
            PasswordResetTokenRepositoryPort tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailServicePort emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Executes the password reset use case.
     *
     * @param request the reset password request containing token and new password
     * @return PasswordResetResponse indicating success or failure
     */
    @Transactional
    public PasswordResetResponse execute(ResetPasswordRequest request) {
        // Find token
        var tokenOptional = tokenRepository.findByToken(request.token());

        if (tokenOptional.isEmpty()) {
            log.warn("Password reset attempted with non-existent token");
            return PasswordResetResponse.invalidToken();
        }

        PasswordResetToken token = tokenOptional.get();

        // Validate token using domain logic
        try {
            token.validateForPasswordReset();
        } catch (IllegalStateException e) {
            log.warn("Password reset attempted with invalid token for user {}: {}", token.userId(), e.getMessage());
            return PasswordResetResponse.invalidToken();
        }

        // Find user
        var userOptional = userRepository.findById(token.userId());
        if (userOptional.isEmpty()) {
            log.error("Token exists for non-existent user {}", token.userId());
            return PasswordResetResponse.error("User account no longer exists");
        }

        User user = userOptional.get();

        try {
            // Hash new password
            String hashedPassword = passwordEncoder.encode(request.newPassword());

            // Update user's password
            User updatedUser = User.builder()
                    .id(user.id())
                    .name(user.name())
                    .email(user.email())
                    .passwordHash(hashedPassword)
                    .phoneNumber(user.phoneNumber())
                    .roles(user.roles())
                    .activePlan(user.activePlan())
                    .capabilities(user.capabilities())
                    .accountVerified(user.isAccountVerified())
                    .createdAt(user.createdAt())
                    .build();

            userRepository.save(updatedUser);

            // Mark token as used
            PasswordResetToken usedToken = token.markAsUsed();
            tokenRepository.save(usedToken);

            // Invalidate all other tokens for this user (security measure)
            int invalidatedCount = tokenRepository.invalidateAllTokensForUser(user.id());
            log.debug("Invalidated {} other token(s) for user {} after password reset", invalidatedCount, user.id());

            // Send confirmation email
            try {
                emailService.sendPasswordResetConfirmationEmail(
                        user.email().address(),
                        user.name()
                );
            } catch (Exception e) {
                // Log but don't fail the operation if email fails
                log.error("Failed to send password reset confirmation email to user {}: {}", user.id(), e.getMessage());
            }

            log.info("Password reset completed successfully for user {}", user.id());

            return PasswordResetResponse.passwordResetSuccess();

        } catch (Exception e) {
            log.error("Failed to reset password for user {}: {}", user.id(), e.getMessage(), e);
            return PasswordResetResponse.error("An error occurred while resetting your password. Please try again.");
        }
    }
}
