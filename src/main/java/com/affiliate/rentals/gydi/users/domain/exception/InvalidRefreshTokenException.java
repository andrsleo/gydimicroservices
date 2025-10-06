package com.affiliate.rentals.gydi.users.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is invalid, expired, or revoked.
 *
 * <p>This exception is mapped to HTTP 401 UNAUTHORIZED, indicating that
 * the client must obtain a new refresh token through re-authentication.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.UNAUTHORIZED, errorType = "Invalid Refresh Token")
public final class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}