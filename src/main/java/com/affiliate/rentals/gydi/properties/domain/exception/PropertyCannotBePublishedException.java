package com.affiliate.rentals.gydi.properties.domain.exception;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when a property cannot be published due to missing required data.
 *
 * <p>Aggregates multiple validation errors for better user experience, allowing users
 * to see all issues at once rather than discovering them one by one.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.BAD_REQUEST, errorType = "Property Cannot Be Published")
public final class PropertyCannotBePublishedException extends PropertyDomainException {

    private static final String DEFAULT_MESSAGE = "Property cannot be published";
    private final List<String> errors;

    /**
     * Creates exception with a single error message.
     *
     * @param message the error message
     */
    public PropertyCannotBePublishedException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    /**
     * Creates exception with multiple validation errors.
     *
     * @param errors list of validation error messages
     */
    public PropertyCannotBePublishedException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns the list of validation errors.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Checks if this exception contains multiple errors.
     *
     * @return true if more than one error, false otherwise
     */
    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }

    /**
     * Builds a comprehensive error message from the list of errors.
     *
     * @param errors list of error messages
     * @return formatted message
     */
    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return DEFAULT_MESSAGE;
        }
        if (errors.size() == 1) {
            return errors.getFirst();
        }
        return String.format("%s. %d issues found: %s",
                DEFAULT_MESSAGE, errors.size(), String.join("; ", errors));
    }
}