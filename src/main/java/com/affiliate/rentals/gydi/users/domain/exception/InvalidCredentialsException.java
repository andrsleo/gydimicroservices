package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 *
 * <p>This exception is part of the sealed {@link DomainException} hierarchy
 * and represents an authentication failure, typically during login when
 * the provided credentials don't match any user or are incorrect.</p>
 *
 * <p>For security reasons, the exception message should not reveal whether
 * the email or password was incorrect.</p>
 *
 * @author GYDI Development Team
 */
public final class InvalidCredentialsException extends DomainException {

    private static final String DEFAULT_MESSAGE = "Invalid email or password";

    /**
     * Constructs a new InvalidCredentialsException with a default message.
     */
    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new InvalidCredentialsException with a custom message.
     *
     * @param message the detail message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}