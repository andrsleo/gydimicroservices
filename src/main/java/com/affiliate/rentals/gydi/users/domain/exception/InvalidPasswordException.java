package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when the current password provided is incorrect.
 *
 * <p>
 * This exception is thrown when a user attempts to change their password
 * but the current password they provide does not match the stored hash.
 * </p>
 *
 * @author GYDI Development Team
 */
public class InvalidPasswordException extends RuntimeException {

    /**
     * Constructs a new InvalidPasswordException with the specified message.
     *
     * @param message the detail message
     */
    public InvalidPasswordException(String message) {
        super(message);
    }

    /**
     * Factory method for creating an exception with a standard message.
     *
     * @return a new InvalidPasswordException
     */
    public static InvalidPasswordException incorrectPassword() {
        return new InvalidPasswordException("Current password is incorrect");
    }
}
