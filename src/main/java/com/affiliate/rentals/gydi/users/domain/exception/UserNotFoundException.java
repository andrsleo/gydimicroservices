package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when a requested user cannot be found.
 *
 * <p>This exception is part of the sealed {@link DomainException} hierarchy
 * and represents a business rule violation where a user operation is attempted
 * on a non-existent user.</p>
 *
 * @author GYDI Development Team
 */
public final class UserNotFoundException extends DomainException {

    private static final String DEFAULT_MESSAGE = "User not found";

    /**
     * Constructs a new UserNotFoundException with a default message.
     */
    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new UserNotFoundException with a custom message.
     *
     * @param message the detail message
     */
    public UserNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new UserNotFoundException for a user identified by ID.
     *
     * @param userId the ID of the user that was not found
     * @return a new UserNotFoundException with a formatted message
     */
    public static UserNotFoundException withId(Long userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    /**
     * Constructs a new UserNotFoundException for a user identified by email.
     *
     * @param email the email of the user that was not found
     * @return a new UserNotFoundException with a formatted message
     */
    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }
}