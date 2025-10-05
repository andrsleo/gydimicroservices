package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 *
 * <p>This exception is part of the sealed {@link DomainException} hierarchy
 * and represents a business rule violation where uniqueness constraints
 * (typically email) are violated during user creation.</p>
 *
 * @author GYDI Development Team
 */
public final class UserAlreadyExistsException extends DomainException {

    private static final String DEFAULT_MESSAGE = "User already exists";

    /**
     * Constructs a new UserAlreadyExistsException with a default message.
     */
    public UserAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new UserAlreadyExistsException with a custom message.
     *
     * @param message the detail message
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new UserAlreadyExistsException for a user with a specific email.
     *
     * @param email the email that already exists
     * @return a new UserAlreadyExistsException with a formatted message
     */
    public static UserAlreadyExistsException withEmail(String email) {
        return new UserAlreadyExistsException("User with email '" + email + "' already exists");
    }
}