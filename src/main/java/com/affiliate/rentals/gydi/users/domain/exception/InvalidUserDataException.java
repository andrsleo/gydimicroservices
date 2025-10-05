package com.affiliate.rentals.gydi.users.domain.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when user data violates domain business rules.
 *
 * <p>This exception is part of the sealed {@link DomainException} hierarchy
 * and represents validation failures at the domain level, such as invalid
 * email formats, weak passwords, or other business rule violations.</p>
 *
 * <p>This exception can aggregate multiple validation errors for better
 * user experience, allowing all issues to be reported at once.</p>
 *
 * @author GYDI Development Team
 */
public final class InvalidUserDataException extends DomainException {

    private static final String DEFAULT_MESSAGE = "Invalid user data";
    private final List<String> errors;

    /**
     * Constructs a new InvalidUserDataException with a single error message.
     *
     * @param message the detail message
     */
    public InvalidUserDataException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    /**
     * Constructs a new InvalidUserDataException with multiple error messages.
     *
     * @param errors the list of validation errors
     */
    public InvalidUserDataException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Gets the list of all validation errors.
     *
     * @return an unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Checks if there are multiple validation errors.
     *
     * @return true if there are multiple errors, false otherwise
     */
    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }

    /**
     * Creates an InvalidUserDataException for an invalid email format.
     *
     * @param email the invalid email
     * @return a new InvalidUserDataException with a formatted message
     */
    public static InvalidUserDataException invalidEmail(String email) {
        return new InvalidUserDataException("Invalid email format: " + email);
    }

    /**
     * Creates an InvalidUserDataException for a weak password.
     *
     * @return a new InvalidUserDataException with a formatted message
     */
    public static InvalidUserDataException weakPassword() {
        return new InvalidUserDataException(
                "Password must be at least 8 characters long and contain uppercase, lowercase, and numbers"
        );
    }

    /**
     * Creates an InvalidUserDataException for an invalid name.
     *
     * @return a new InvalidUserDataException with a formatted message
     */
    public static InvalidUserDataException invalidName() {
        return new InvalidUserDataException("Name must not be blank and must be between 2 and 255 characters");
    }

    /**
     * Builds a composite message from multiple errors.
     *
     * @param errors the list of errors
     * @return a formatted error message
     */
    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return DEFAULT_MESSAGE;
        }
        if (errors.size() == 1) {
            return errors.getFirst();
        }
        return "Multiple validation errors: " + String.join("; ", errors);
    }
}