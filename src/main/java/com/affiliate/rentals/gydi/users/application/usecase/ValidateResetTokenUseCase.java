package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.ValidateTokenResponse;
import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.PasswordResetTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for validating a password reset token.
 *
 * <p>This service handles the business logic for validating a password reset token before
 * allowing the user to set a new password. It checks:</p>
 * <ul>
 *   <li>Token exists in the database</li>
 *   <li>Token has not expired</li>
 *   <li>Token has not been used</li>
 *   <li>Associated user still exists</li>
 * </ul>
 *
 * <p>This endpoint is typically called when:</p>
 * <ul>
 *   <li>User clicks the reset link in their email</li>
 *   <li>Frontend needs to verify token validity before showing password form</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class ValidateResetTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValidateResetTokenUseCase.class);

    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final UserRepositoryPort userRepository;

    public ValidateResetTokenUseCase(
            PasswordResetTokenRepositoryPort tokenRepository,
            UserRepositoryPort userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Executes the token validation use case.
     *
     * @param tokenString the token string to validate
     * @return ValidateTokenResponse containing validation result and user email if valid
     */
    @Transactional(readOnly = true)
    public ValidateTokenResponse execute(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            log.warn("Validation attempted with blank token");
            return ValidateTokenResponse.notFound();
        }

        var tokenOptional = tokenRepository.findByToken(tokenString);

        if (tokenOptional.isEmpty()) {
            log.warn("Validation attempted with non-existent token");
            return ValidateTokenResponse.notFound();
        }

        PasswordResetToken token = tokenOptional.get();

        // Check if token has expired
        if (token.isExpired()) {
            log.info("Validation attempted with expired token for user {}", token.userId());
            return ValidateTokenResponse.expired();
        }

        // Check if token has already been used
        if (token.isUsed()) {
            log.info("Validation attempted with already used token for user {}", token.userId());
            return ValidateTokenResponse.alreadyUsed();
        }

        // Verify that the user still exists
        var userOptional = userRepository.findById(token.userId());
        if (userOptional.isEmpty()) {
            log.error("Token exists for non-existent user {}", token.userId());
            return ValidateTokenResponse.invalid("User account no longer exists");
        }

        User user = userOptional.get();

        log.debug("Token validated successfully for user {}", user.id());

        return ValidateTokenResponse.valid(
                user.email().address(),
                token.expiresAt()
        );
    }
}
