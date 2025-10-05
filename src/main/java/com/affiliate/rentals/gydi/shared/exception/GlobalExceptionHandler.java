package com.affiliate.rentals.gydi.shared.exception;

import com.affiliate.rentals.gydi.users.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>This class provides centralized exception handling across all controllers
 * using Spring's @RestControllerAdvice. It leverages Java 21's pattern matching
 * for switch expressions and sealed classes for exhaustive exception handling.</p>
 *
 * <p>Exception handling strategy:</p>
 * <ul>
 *   <li>Domain exceptions (4xx) - Business rule violations</li>
 *   <li>Validation exceptions (400) - Request validation failures</li>
 *   <li>Authentication exceptions (401) - Authentication failures</li>
 *   <li>Generic exceptions (500) - Unexpected server errors</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all domain-level exceptions from the users context.
     *
     * <p>Uses Java 21's pattern matching for switch with sealed classes
     * to provide exhaustive handling of all domain exception types.</p>
     *
     * @param ex      the domain exception
     * @param request the HTTP request
     * @return an appropriate error response based on exception type
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex,
            HttpServletRequest request
    ) {
        logger.warn("Domain exception: {}", ex.getMessage());

        // Pattern matching for switch with sealed classes (Java 21)
        return switch (ex) {
            case UserNotFoundException e -> buildResponse(
                    HttpStatus.NOT_FOUND,
                    "User Not Found",
                    e.getMessage(),
                    request.getRequestURI()
            );

            case UserAlreadyExistsException e -> buildResponse(
                    HttpStatus.CONFLICT,
                    "User Already Exists",
                    e.getMessage(),
                    request.getRequestURI()
            );

            case InvalidCredentialsException e -> buildResponse(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Credentials",
                    e.getMessage(),
                    request.getRequestURI()
            );

            case InvalidUserDataException e -> {
                if (e.hasMultipleErrors()) {
                    List<ErrorResponse.FieldError> fieldErrors = e.getErrors().stream()
                            .map(error -> ErrorResponse.FieldError.of("user", error))
                            .toList();

                    yield ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(ErrorResponse.of(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid User Data",
                                    e.getMessage(),
                                    request.getRequestURI(),
                                    fieldErrors
                            ));
                }

                yield buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid User Data",
                        e.getMessage(),
                        request.getRequestURI()
                );
            }
        };
    }

    /**
     * Handles Bean Validation exceptions (e.g., @Valid, @Validated).
     *
     * <p>Transforms Spring's validation errors into a standardized error response
     * with field-level error details.</p>
     *
     * @param ex      the validation exception
     * @param request the HTTP request
     * @return a BAD_REQUEST response with field-level errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        logger.warn("Validation exception: {}", ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed. Please check the provided data.",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles constraint violation exceptions from Bean Validation.
     *
     * @param ex      the constraint violation exception
     * @param request the HTTP request
     * @return a BAD_REQUEST response with validation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        logger.warn("Constraint violation: {}", ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .toList();

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                "Validation constraint violated",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles authentication exceptions (e.g., from Spring Security).
     *
     * @param ex      the authentication exception
     * @param request the HTTP request
     * @return an UNAUTHORIZED response
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.warn("Authentication exception: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Invalid credentials or authentication token",
                request.getRequestURI()
        );
    }

    /**
     * Handles method argument type mismatch exceptions.
     *
     * @param ex      the type mismatch exception
     * @param request the HTTP request
     * @return a BAD_REQUEST response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        logger.warn("Type mismatch exception: {}", ex.getMessage());

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter Type",
                message,
                request.getRequestURI()
        );
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * <p>Logs the full stack trace and returns a generic error response
     * to avoid leaking sensitive information to clients.</p>
     *
     * @param ex      the unexpected exception
     * @param request the HTTP request
     * @return an INTERNAL_SERVER_ERROR response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error("Unexpected exception occurred", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
    }

    /**
     * Builds a standardized ResponseEntity with ErrorResponse.
     *
     * @param status  the HTTP status
     * @param error   the error type
     * @param message the error message
     * @param path    the request path
     * @return a ResponseEntity with the error response
     */
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                error,
                message,
                path
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Converts a Spring FieldError to our FieldError record.
     *
     * @param fieldError the Spring field error
     * @return our FieldError record
     */
    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.of(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
        );
    }

    /**
     * Converts a Bean Validation ConstraintViolation to our FieldError record.
     *
     * @param violation the constraint violation
     * @return our FieldError record
     */
    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        String fieldName = violation.getPropertyPath().toString();
        return ErrorResponse.FieldError.of(
                fieldName,
                violation.getInvalidValue(),
                violation.getMessage()
        );
    }
}