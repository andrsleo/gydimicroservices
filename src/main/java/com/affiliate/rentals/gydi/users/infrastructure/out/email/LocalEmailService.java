package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Local email service implementation for development.
 *
 * <p>
 * This service does NOT send actual emails. Instead, it logs email details to the console,
 * allowing developers to test email functionality without requiring email service credentials.
 * </p>
 *
 * <p>
 * <b>Active when:</b> {@code email.provider=local}
 * </p>
 *
 * <p>
 * <b>Configuration in application.yml:</b>
 * <pre>
 * email:
 *   provider: local  # Activates this service
 * </pre>
 * </p>
 *
 * <p>
 * <b>Benefits:</b>
 * <ul>
 *   <li>No email credentials needed</li>
 *   <li>No email service costs</li>
 *   <li>Fast development iteration</li>
 *   <li>Easy to debug email content</li>
 *   <li>View email details directly in console logs</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Example Log Output:</b>
 * <pre>
 * ═══════════════════════════════════════════════════════════
 * 📧 LOCAL EMAIL SERVICE - Password Reset Email
 * ═══════════════════════════════════════════════════════════
 * To: user@example.com
 * Subject: Reset Your GYDI Password
 * User: John Doe
 * Reset Link: http://localhost:3000/reset-password?token=abc123
 * ═══════════════════════════════════════════════════════════
 * </pre>
 * </p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "email.provider", havingValue = "local", matchIfMissing = true)
public class LocalEmailService implements EmailServicePort {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
        log.info("\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "📧 LOCAL EMAIL SERVICE - Password Reset Email\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "To: {}\n" +
            "Subject: Reset Your GYDI Password\n" +
            "User: {}\n" +
            "Reset Link: {}\n" +
            "═══════════════════════════════════════════════════════════",
            toEmail, userName, resetLink
        );
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        log.info("\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "📧 LOCAL EMAIL SERVICE - Password Reset Confirmation\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "To: {}\n" +
            "Subject: Your GYDI Password Was Changed\n" +
            "User: {}\n" +
            "Message: Your password was successfully changed.\n" +
            "═══════════════════════════════════════════════════════════",
            toEmail, userName
        );
    }

    @Override
    public void sendSecurityAlertEmail(String toEmail, String userName, String activityDescription) {
        log.warn("\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "🚨 LOCAL EMAIL SERVICE - Security Alert\n" +
            "═══════════════════════════════════════════════════════════\n" +
            "To: {}\n" +
            "Subject: Security Alert - GYDI Account Activity\n" +
            "User: {}\n" +
            "Activity: {}\n" +
            "═══════════════════════════════════════════════════════════",
            toEmail, userName, activityDescription
        );
    }
}
