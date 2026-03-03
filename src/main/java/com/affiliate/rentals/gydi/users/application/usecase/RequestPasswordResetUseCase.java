package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.users.application.dto.ForgotPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.PasswordResetResponse;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.PasswordResetTokenRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.UserEmailTemplateFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Use case for requesting a password reset.
 *
 * <p>
 * This service handles the business logic for initiating a password reset
 * process, including:
 * </p>
 * <ul>
 * <li>Validating the user exists</li>
 * <li>Generating a secure reset token</li>
 * <li>Invalidating any existing tokens for the user</li>
 * <li>Sending the reset email</li>
 * </ul>
 *
 * <p>
 * Security considerations:
 * </p>
 * <ul>
 * <li>Always returns success message regardless of email existence (prevents
 * enumeration)</li>
 * <li>Logs failed attempts for security monitoring</li>
 * <li>Rate limiting is enforced at the controller/AOP level</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
public class RequestPasswordResetUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final EmailServicePort emailService;
    private final UserEmailTemplateFactory templateFactory;

    @Value("${app.password-reset.token-expiration-minutes:60}")
    private int tokenExpirationMinutes;

    @Value("${app.password-reset.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public RequestPasswordResetUseCase(
            UserRepositoryPort userRepository,
            PasswordResetTokenRepositoryPort tokenRepository,
            EmailServicePort emailService,
            UserEmailTemplateFactory templateFactory) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.templateFactory = templateFactory;
    }

    /**
     * Executes the password reset request use case.
     *
     * <p>
     * If the user exists:
     * </p>
     * <ol>
     * <li>Invalidates all existing tokens for the user</li>
     * <li>Generates a new reset token</li>
     * <li>Sends password reset email</li>
     * </ol>
     *
     * <p>
     * If the user doesn't exist, silently succeeds to prevent email enumeration.
     * </p>
     *
     * @param request   the forgot password request containing the email
     * @param ipAddress the IP address of the requester (for audit)
     * @return a generic success response
     */
    @Transactional
    public PasswordResetResponse execute(ForgotPasswordRequest request, String ipAddress) {
        Email email = Email.of(request.email());

        var userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // Don't reveal that the email doesn't exist
            log.info("Password reset requested for non-existent email: {}", request.email());
            return PasswordResetResponse.resetRequested();
        }

        User user = userOptional.get();

        try {
            // Invalidate all existing tokens for this user
            int invalidatedCount = tokenRepository.invalidateAllTokensForUser(user.id());
            if (invalidatedCount > 0) {
                log.debug("Invalidated {} existing token(s) for user {}", invalidatedCount, user.id());
            }

            // Generate new reset token
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.id())
                    .expirationMinutes(tokenExpirationMinutes)
                    .ipAddress(ipAddress)
                    .build();

            PasswordResetToken savedToken = tokenRepository.save(token);

            // Build reset link
            String resetLink = String.format(
                    "%s/reset-password?token=%s",
                    frontendUrl,
                    savedToken.token());

            // 1. Build the HTML string using the Factory
            String htmlTemplate = templateFactory.buildPasswordResetHtml(user.name(), resetLink);

            // 2. Wrap it inside the generic EmailMessage
            EmailMessage emailMessage = new EmailMessage(
                    email.toString(), // Real recipient
                    "Password Reset - GYDI", // Subject
                    htmlTemplate, // HTML built by factory
                    null, null, null);

            // 3. Send using transversal port
            emailService.sendEmail(emailMessage);

            log.info("Password reset email sent successfully to user {}", user.id());

        } catch (Exception e) {
            // Log the error but don't expose it to the user
            log.error("Failed to process password reset for user {}: {}", user.id(), e.getMessage(), e);
            // Still return generic success message
        }

        // Always return generic success message to prevent email enumeration
        return PasswordResetResponse.resetRequested();
    }
}
