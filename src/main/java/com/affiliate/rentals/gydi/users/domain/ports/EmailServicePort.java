package com.affiliate.rentals.gydi.users.domain.ports;

/**
 * Port interface for email sending operations.
 *
 * <p>This port defines the contract for sending emails in the application.
 * It follows the hexagonal architecture pattern, allowing the domain layer to remain independent
 * of specific email service implementations (e.g., Amazon SES, SendGrid, SMTP).</p>
 *
 * <p>Implementations of this interface will be provided by adapters in the infrastructure layer
 * that integrate with actual email service providers.</p>
 *
 * <p>Example usage in use cases:</p>
 * <pre>{@code
 * public class RequestPasswordResetUseCase {
 *     private final EmailServicePort emailService;
 *
 *     public void execute(String userEmail, String resetToken) {
 *         String resetLink = "https://app.gydi.com/reset-password?token=" + resetToken;
 *         emailService.sendPasswordResetEmail(userEmail, resetLink, "John Doe");
 *     }
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
public interface EmailServicePort {

    /**
     * Sends a password reset email to the specified recipient.
     *
     * <p>The email should contain a link to reset the password, which includes the reset token.
     * The link should be valid for a limited time (typically 1 hour).</p>
     *
     * <p>Email content should include:</p>
     * <ul>
     *   <li>A clear call-to-action button/link for password reset</li>
     *   <li>Token expiration information</li>
     *   <li>Security notice about ignoring the email if not requested</li>
     *   <li>Company branding and contact information</li>
     * </ul>
     *
     * @param toEmail the recipient's email address
     * @param resetLink the complete URL for password reset (includes token)
     * @param userName the recipient's name for personalization
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws EmailSendingException if the email fails to send
     */
    void sendPasswordResetEmail(String toEmail, String resetLink, String userName);

    /**
     * Sends a password reset confirmation email after successful password change.
     *
     * <p>This notification confirms that the password was successfully changed and provides
     * security information about what to do if the change was unauthorized.</p>
     *
     * @param toEmail the recipient's email address
     * @param userName the recipient's name for personalization
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws EmailSendingException if the email fails to send
     */
    void sendPasswordResetConfirmationEmail(String toEmail, String userName);

    /**
     * Sends a security alert email when suspicious activity is detected.
     *
     * <p>This is sent when:</p>
     * <ul>
     *   <li>Multiple failed password reset attempts are detected</li>
     *   <li>Rate limits are exceeded</li>
     *   <li>Password reset is attempted from unusual location/IP</li>
     * </ul>
     *
     * @param toEmail the recipient's email address
     * @param userName the recipient's name for personalization
     * @param activityDescription description of the suspicious activity
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws EmailSendingException if the email fails to send
     */
    void sendSecurityAlertEmail(String toEmail, String userName, String activityDescription);

    /**
     * Custom exception for email sending failures.
     * Implementations should throw this exception when an email cannot be sent.
     */
    class EmailSendingException extends RuntimeException {
        public EmailSendingException(String message) {
            super(message);
        }

        public EmailSendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
