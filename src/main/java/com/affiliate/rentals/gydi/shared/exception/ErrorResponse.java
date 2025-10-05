package com.affiliate.rentals.gydi.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response structure for API errors.
 *
 * <p>This record (Java 16+) provides an immutable, thread-safe error response
 * format that is consistent across the entire API. It includes timestamp,
 * status code, error type, message, and optional field-level errors.</p>
 *
 * <p>Uses Jackson's @JsonInclude to omit null fields from JSON responses.</p>
 *
 * @param timestamp the instant when the error occurred
 * @param status    the HTTP status code
 * @param error     the error type/category
 * @param message   the error message
 * @param path      the request path that caused the error
 * @param errors    optional list of field-level validation errors
 * @author GYDI Development Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors
) {
    /**
     * Creates an ErrorResponse without field-level errors.
     *
     * @param status  the HTTP status code
     * @param error   the error type
     * @param message the error message
     * @param path    the request path
     * @return a new ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                null
        );
    }

    /**
     * Creates an ErrorResponse with field-level errors.
     *
     * @param status  the HTTP status code
     * @param error   the error type
     * @param message the error message
     * @param path    the request path
     * @param errors  the list of field errors
     * @return a new ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> errors) {
        return new ErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                errors
        );
    }

    /**
     * Represents a field-level validation error.
     *
     * @param field         the name of the field that failed validation
     * @param rejectedValue the value that was rejected
     * @param message       the validation error message
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(
            String field,
            Object rejectedValue,
            String message
    ) {
        /**
         * Creates a FieldError without the rejected value.
         *
         * @param field   the field name
         * @param message the error message
         * @return a new FieldError
         */
        public static FieldError of(String field, String message) {
            return new FieldError(field, null, message);
        }

        /**
         * Creates a FieldError with the rejected value.
         *
         * @param field         the field name
         * @param rejectedValue the rejected value
         * @param message       the error message
         * @return a new FieldError
         */
        public static FieldError of(String field, Object rejectedValue, String message) {
            return new FieldError(field, rejectedValue, message);
        }
    }
}