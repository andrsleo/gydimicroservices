package com.affiliate.rentals.gydi.shared.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.affiliate.rentals.gydi.properties.domain.exception.PropertyCannotBePublishedException;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyDomainException;
import com.affiliate.rentals.gydi.users.domain.exception.DomainException;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidPasswordException;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidTokenException;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidUserDataException;
import com.affiliate.rentals.gydi.users.domain.exception.TokenNotFoundException;
import com.affiliate.rentals.gydi.users.domain.exception.RateLimitExceededException;
import com.affiliate.rentals.gydi.users.domain.exception.MissingRefreshTokenException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>
 * This class provides centralized exception handling across all controllers
 * using Spring's @RestControllerAdvice. It leverages Java 21's pattern matching
 * for switch expressions and sealed classes for exhaustive exception handling.
 * </p>
 *
 * <p>
 * Exception handling strategy:
 * </p>
 * <ul>
 * <li>Domain exceptions (4xx) - Business rule violations</li>
 * <li>Validation exceptions (400) - Request validation failures</li>
 * <li>Authentication exceptions (401) - Authentication failures</li>
 * <li>Generic exceptions (500) - Unexpected server errors</li>
 * </ul>
 *
 * @author GYDI Development Team
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles all domain-level exceptions using annotation-based HTTP status
         * mapping.
         *
         * <p>
         * This method uses reflection to dynamically extract HTTP status and error type
         * from the {@link HttpStatusMapping} annotation on exception classes. This
         * approach
         * eliminates the need for explicit switch statements and allows new exception
         * types
         * to be added without modifying this handler.
         * </p>
         *
         * @param ex      the domain exception
         * @param request the HTTP request
         * @return an appropriate error response based on the exception's annotation
         */
        @ExceptionHandler(DomainException.class)
        public ResponseEntity<ErrorResponse> handleDomainException(
                        DomainException ex,
                        HttpServletRequest request) {
                log.warn("Domain exception: {}", ex.getMessage());

                // Extract HTTP status and error type from annotation
                HttpStatusMapping mapping = ex.getClass().getAnnotation(HttpStatusMapping.class);

                if (mapping == null) {
                        log.error("Domain exception {} is missing @HttpStatusMapping annotation",
                                        ex.getClass().getSimpleName());
                        return buildResponse(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Internal Error",
                                        "An unexpected error occurred",
                                        request.getRequestURI());
                }

                HttpStatus status = mapping.status();
                String errorType = mapping.errorType();

                // Handle special case for InvalidUserDataException with multiple errors
                if (ex instanceof InvalidUserDataException dataException && dataException.hasMultipleErrors()) {
                        List<ErrorResponse.FieldError> fieldErrors = dataException.getErrors().stream()
                                        .map(error -> ErrorResponse.FieldError.of("user", error))
                                        .toList();

                        return ResponseEntity
                                        .status(status)
                                        .body(ErrorResponse.of(
                                                        status.value(),
                                                        errorType,
                                                        ex.getMessage(),
                                                        request.getRequestURI(),
                                                        fieldErrors));
                }

                return buildResponse(status, errorType, ex.getMessage(), request.getRequestURI());
        }

        /**
         * Handles property domain exceptions using annotation-based HTTP status
         * mapping.
         *
         * <p>
         * This handler specifically manages exceptions from the properties bounded
         * context,
         * following the same pattern as the users domain exception handler. It extracts
         * HTTP status and error type from the {@link HttpStatusMapping} annotation.
         * </p>
         *
         * <p>
         * Special handling for {@link PropertyCannotBePublishedException} which
         * aggregates
         * multiple validation errors, providing detailed feedback to users about all
         * missing
         * requirements for publication.
         * </p>
         *
         * @param ex      the property domain exception
         * @param request the HTTP request
         * @return an appropriate error response based on the exception's annotation
         */
        @ExceptionHandler(PropertyDomainException.class)
        public ResponseEntity<ErrorResponse> handlePropertyDomainException(
                        PropertyDomainException ex,
                        HttpServletRequest request) {
                log.warn("Property domain exception: {}", ex.getMessage());

                // Extract HTTP status and error type from annotation
                HttpStatusMapping mapping = ex.getClass().getAnnotation(HttpStatusMapping.class);

                if (mapping == null) {
                        log.error("Property domain exception {} is missing @HttpStatusMapping annotation",
                                        ex.getClass().getSimpleName());
                        return buildResponse(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Internal Error",
                                        "An unexpected error occurred",
                                        request.getRequestURI());
                }

                HttpStatus status = mapping.status();
                String errorType = mapping.errorType();

                // Handle special case for PropertyCannotBePublishedException with multiple
                // errors
                if (ex instanceof PropertyCannotBePublishedException publishException &&
                                publishException.hasMultipleErrors()) {
                        List<ErrorResponse.FieldError> fieldErrors = publishException.getErrors().stream()
                                        .map(error -> ErrorResponse.FieldError.of("property", error))
                                        .toList();

                        return ResponseEntity
                                        .status(status)
                                        .body(ErrorResponse.of(
                                                        status.value(),
                                                        errorType,
                                                        ex.getMessage(),
                                                        request.getRequestURI(),
                                                        fieldErrors));
                }

                return buildResponse(status, errorType, ex.getMessage(), request.getRequestURI());
        }

        /**
         * Handles Bean Validation exceptions (e.g., @Valid, @Validated).
         *
         * <p>
         * Transforms Spring's validation errors into a standardized error response
         * with field-level error details.
         * </p>
         *
         * @param ex      the validation exception
         * @param request the HTTP request
         * @return a BAD_REQUEST response with field-level errors
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                log.warn("Validation exception: {}", ex.getMessage());

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
                                fieldErrors);

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
                        HttpServletRequest request) {
                log.warn("Constraint violation: {}", ex.getMessage());

                List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                                .stream()
                                .map(this::toFieldError)
                                .toList();

                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Constraint Violation",
                                "Validation constraint violated",
                                request.getRequestURI(),
                                fieldErrors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        /**
         * Handles authentication exceptions (e.g., from Spring Security).
         *
         * @param ex      the authentication exception
         * @param request the HTTP request
         * @return an UNAUTHORIZED response
         */
        @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        Exception ex,
                        HttpServletRequest request) {
                log.warn("Authentication exception: {}", ex.getMessage());

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication Failed",
                                "Invalid credentials or authentication token",
                                request.getRequestURI());
        }

        /**
         * Handles forbidden access exceptions (IDOR prevention).
         *
         * <p>
         * <b>SECURITY: IDOR (Insecure Direct Object Reference) Prevention</b>
         * </p>
         *
         * <p>
         * This handler catches attempts by users to access resources they don't own.
         * It returns HTTP 403 Forbidden instead of 404 Not Found to be transparent
         * about
         * the reason for denial (resource exists but user doesn't have permission).
         * </p>
         *
         * @param ex      the forbidden exception
         * @param request the HTTP request
         * @return a FORBIDDEN response
         */
        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ErrorResponse> handleForbiddenException(
                        ForbiddenException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: Forbidden access attempt - {}", ex.getMessage());

                return buildResponse(
                                HttpStatus.FORBIDDEN,
                                "Access Denied",
                                ex.getMessage(),
                                request.getRequestURI());
        }

        /**
         * Handles invalid password exceptions during password change operations.
         *
         * <p>
         * <b>SECURITY: Password Verification</b>
         * </p>
         *
         * <p>
         * This handler catches attempts to change password with incorrect current
         * password.
         * It returns HTTP 400 Bad Request to indicate that the provided current
         * password
         * does not match the stored password.
         * </p>
         *
         * @param ex      the invalid password exception
         * @param request the HTTP request
         * @return a BAD_REQUEST response
         */
        @ExceptionHandler(InvalidPasswordException.class)
        public ResponseEntity<ErrorResponse> handleInvalidPasswordException(
                        InvalidPasswordException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: Invalid password attempt - {}", ex.getMessage());

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid Password",
                                ex.getMessage(),
                                request.getRequestURI());
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
                        HttpServletRequest request) {
                log.warn("Type mismatch exception: {}", ex.getMessage());

                String message = String.format(
                                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                                ex.getValue(),
                                ex.getName(),
                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid Parameter Type",
                                message,
                                request.getRequestURI());
        }

        /**
         * Handles illegal state exceptions (e.g., business rule violations).
         *
         * <p>
         * This is commonly thrown when an operation cannot be performed
         * due to the current state of an entity, such as trying to publish
         * a property that doesn't meet the minimum requirements.
         * </p>
         *
         * @param ex      the illegal state exception
         * @param request the HTTP request
         * @return a BAD_REQUEST response
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalStateException(
                        IllegalStateException ex,
                        HttpServletRequest request) {
                log.warn("Illegal state exception: {}", ex.getMessage());

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid Operation",
                                ex.getMessage(),
                                request.getRequestURI());
        }

        /**
         * Handles illegal argument exceptions.
         *
         * <p>
         * This is thrown when a method receives an argument that is inappropriate
         * or incorrect, such as an invalid iCal URL.
         * </p>
         *
         * @param ex      the illegal argument exception
         * @param request the HTTP request
         * @return a BAD_REQUEST response
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                log.warn("Illegal argument exception: {}", ex.getMessage());

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid Argument",
                                ex.getMessage(),
                                request.getRequestURI());
        }

        /**
         * Handles ResponseStatusException thrown by controllers.
         *
         * <p>
         * This handler specifically manages Spring's ResponseStatusException,
         * which is commonly used for HTTP status codes like 429 (Too Many Requests).
         * We log at WARN level for expected client errors (4xx) and ERROR for
         * unexpected server errors (5xx).
         * </p>
         *
         * @param ex      the response status exception
         * @param request the HTTP request
         * @return an appropriate error response based on the exception's status
         */
        @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatusException(
                        org.springframework.web.server.ResponseStatusException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

                // Log at WARN for client errors (4xx), ERROR for server errors (5xx)
                if (status.is4xxClientError()) {
                        log.warn("Client error {}: {}", status.value(), ex.getReason());
                } else {
                        log.error("Server error {}: {}", status.value(), ex.getReason());
                }

                String errorType = switch (status) {
                        case TOO_MANY_REQUESTS -> "Rate Limit Exceeded";
                        case UNAUTHORIZED -> "Unauthorized";
                        case FORBIDDEN -> "Forbidden";
                        case NOT_FOUND -> "Not Found";
                        case BAD_REQUEST -> "Bad Request";
                        default -> status.getReasonPhrase();
                };

                return buildResponse(
                                status,
                                errorType,
                                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                                request.getRequestURI());
        }

        /**
         * Handles client abort exceptions during streaming (e.g., video, large files).
         *
         * <p>
         * This exception is thrown when the client (browser) closes the connection
         * before the server finishes sending data. This is normal behavior when:
         * <ul>
         * <li>User navigates away while video is loading</li>
         * <li>Video player makes range requests and cancels previous ones</li>
         * <li>Browser prefetches content and then cancels</li>
         * </ul>
         *
         * <p>
         * We log at DEBUG level only and don't return a response since the client
         * has already closed the connection.
         * </p>
         *
         * @param ex      the client abort exception
         * @param request the HTTP request
         */
        @ExceptionHandler(org.apache.catalina.connector.ClientAbortException.class)
        public void handleClientAbortException(
                        org.apache.catalina.connector.ClientAbortException ex,
                        HttpServletRequest request) {
                // Log at DEBUG level only - this is expected behavior during video streaming
                log.debug("Client aborted connection during streaming: {} - {}",
                                request.getMethod(), request.getRequestURI());

                // No response needed - client already closed the connection
        }

        /**
         * Handles all other unexpected exceptions.
         *
         * <p>
         * Logs the full stack trace and returns a generic error response
         * to avoid leaking sensitive information to clients.
         * </p>
         *
         * @param ex      the unexpected exception
         * @param request the HTTP request
         * @return an INTERNAL_SERVER_ERROR response
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                log.error("Unexpected exception occurred", ex);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Internal Server Error",
                                "An unexpected error occurred. Please try again later.",
                                request.getRequestURI());
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
                        String path) {
                ErrorResponse errorResponse = ErrorResponse.of(
                                status.value(),
                                error,
                                message,
                                path);
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
                                fieldError.getDefaultMessage());
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
                                violation.getMessage());
        }

        /**
         * Handles token not found exceptions during authentication.
         *
         * <p>
         * <b>SECURITY: Missing Authentication Token</b>
         * </p>
         *
         * <p>
         * This handler catches attempts to access protected resources without providing
         * an authentication token. It returns HTTP 401 Unauthorized to indicate that
         * authentication is required.
         * </p>
         *
         * @param ex      the token not found exception
         * @param request the HTTP request
         * @return an UNAUTHORIZED response
         */
        @ExceptionHandler(TokenNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTokenNotFound(
                        TokenNotFoundException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: Token not found - {}", ex.getMessage());
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication Required",
                                "No valid authentication token found. Please login.",
                                request.getRequestURI());
        }

        /**
         * Handles invalid token exceptions during authentication.
         *
         * <p>
         * <b>SECURITY: Invalid or Expired Token</b>
         * </p>
         *
         * <p>
         * This handler catches attempts to use invalid, expired, or malformed JWT tokens.
         * It returns HTTP 401 Unauthorized to indicate that the user needs to login again.
         * </p>
         *
         * @param ex      the invalid token exception
         * @param request the HTTP request
         * @return an UNAUTHORIZED response
         */
        @ExceptionHandler(InvalidTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidToken(
                        InvalidTokenException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: Invalid token - {}", ex.getMessage());
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid Token",
                                "Your authentication token is invalid or expired. Please login again.",
                                request.getRequestURI());
        }

        /**
         * Handles missing refresh token exceptions.
         *
         * <p>
         * <b>SECURITY: Missing Refresh Token</b>
         * </p>
         *
         * <p>
         * This handler catches attempts to refresh authentication without providing
         * a refresh token. It returns HTTP 401 Unauthorized with a clear message.
         * </p>
         *
         * @param ex      the missing refresh token exception
         * @param request the HTTP request
         * @return an UNAUTHORIZED response
         */
        @ExceptionHandler(MissingRefreshTokenException.class)
        public ResponseEntity<ErrorResponse> handleMissingRefreshToken(
                        MissingRefreshTokenException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: Missing refresh token - {}", ex.getMessage());
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Missing Refresh Token",
                                ex.getMessage(),
                                request.getRequestURI());
        }

        /**
         * Handles rate limit exceeded exceptions during authentication.
         *
         * <p>
         * <b>SECURITY: Rate Limiting - Brute Force Prevention</b>
         * </p>
         *
         * <p>
         * This handler catches attempts to exceed the allowed rate limit for authentication.
         * It returns HTTP 429 (Too Many Requests) with appropriate retry-after headers
         * to inform the client when they can retry.
         * </p>
         *
         * @param ex       the rate limit exceeded exception
         * @param request  the HTTP request
         * @param response the HTTP response for setting headers
         * @return a TOO_MANY_REQUESTS response
         */
        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
                        RateLimitExceededException ex,
                        HttpServletRequest request,
                        HttpServletResponse response) {
                log.warn("SECURITY: Rate limit exceeded - {}", ex.getMessage());

                // Set rate limit headers for client transparency
                response.setHeader("X-RateLimit-Remaining", String.valueOf(ex.getRemainingAttempts()));
                response.setHeader("X-RateLimit-Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
                response.setHeader("X-RateLimit-Limit", "10");

                return buildResponse(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Rate Limit Exceeded",
                                ex.getMessage(),
                                request.getRequestURI());
        }

        /**
         * Handles JWT-related exceptions from the JJWT library.
         *
         * <p>
         * <b>SECURITY: JWT Processing Errors</b>
         * </p>
         *
         * <p>
         * This handler catches low-level JWT processing exceptions such as:
         * </p>
         * <ul>
         * <li>Malformed JWT tokens</li>
         * <li>Expired tokens</li>
         * <li>Invalid signatures</li>
         * <li>Unsupported JWT operations</li>
         * </ul>
         *
         * <p>
         * It returns HTTP 401 Unauthorized to indicate authentication failure.
         * </p>
         *
         * @param ex      the JWT exception
         * @param request the HTTP request
         * @return an UNAUTHORIZED response
         */
        @ExceptionHandler(JwtException.class)
        public ResponseEntity<ErrorResponse> handleJwtException(
                        JwtException ex,
                        HttpServletRequest request) {
                log.warn("SECURITY: JWT processing error - {}", ex.getMessage());
                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid Token",
                                "Your authentication token is invalid or expired. Please login again.",
                                request.getRequestURI());
        }
}