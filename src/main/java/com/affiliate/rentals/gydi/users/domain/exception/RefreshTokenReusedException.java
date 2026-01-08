package com.affiliate.rentals.gydi.users.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is reused, indicating a potential security breach.
 *
 * <p>This exception is thrown when a token that has already been used for rotation
 * is presented again. This is a strong indicator of token theft, as legitimate clients
 * should only use each refresh token once.</p>
 *
 * <p><b>Security Response:</b></p>
 * <ul>
 *   <li>The entire token family is immediately revoked</li>
 *   <li>The user must re-authenticate to obtain new tokens</li>
 *   <li>The security event is logged for monitoring</li>
 * </ul>
 *
 * <p>This exception is mapped to HTTP 401 UNAUTHORIZED, requiring the client
 * to re-authenticate.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.UNAUTHORIZED, errorType = "Token Reuse Detected")
public final class RefreshTokenReusedException extends DomainException {

    /**
     * Constructs a new RefreshTokenReusedException with a detail message.
     *
     * @param message the detail message explaining the token reuse
     */
    public RefreshTokenReusedException(String message) {
        super(message);
    }

    /**
     * Constructs a new RefreshTokenReusedException with a detail message and cause.
     *
     * @param message the detail message explaining the token reuse
     * @param cause the cause of the exception
     */
    public RefreshTokenReusedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a RefreshTokenReusedException for a specific token.
     *
     * @param tokenId the ID of the reused token
     * @param userId the user ID associated with the token
     * @return a new RefreshTokenReusedException instance
     */
    public static RefreshTokenReusedException forToken(Long tokenId, Long userId) {
        return new RefreshTokenReusedException(
            String.format("SECURITY: Refresh token reuse detected! Token ID: %d, User ID: %d. " +
                "Entire token family has been revoked. User must re-authenticate.",
                tokenId, userId)
        );
    }
}
