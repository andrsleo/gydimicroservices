package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when a refresh token is required but not provided.
 *
 * <p>This exception is thrown when an endpoint expects a refresh token
 * (either in the request body for development or in httpOnly cookies for production)
 * but none is found.</p>
 *
 * <p>This exception extends {@link AuthenticationException} and is handled by the
 * GlobalExceptionHandler to return HTTP 401 (Unauthorized) with an appropriate message.</p>
 *
 * @author GYDI Development Team
 */
public class MissingRefreshTokenException extends AuthenticationException {

    private static final String DEFAULT_MESSAGE = "No refresh token found. Please login again.";

    /**
     * Constructs a new MissingRefreshTokenException with a default message.
     */
    public MissingRefreshTokenException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new MissingRefreshTokenException with a custom message.
     *
     * @param message the detail message
     */
    public MissingRefreshTokenException(String message) {
        super(message);
    }
}
