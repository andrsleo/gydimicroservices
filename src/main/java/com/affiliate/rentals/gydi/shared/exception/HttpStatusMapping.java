package com.affiliate.rentals.gydi.shared.exception;

import org.springframework.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to map exceptions to HTTP status codes and error types.
 *
 * <p>This annotation provides a declarative way to define HTTP response
 * characteristics for domain exceptions, eliminating the need for explicit
 * switch statements in exception handlers.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @HttpStatusMapping(status = HttpStatus.NOT_FOUND, errorType = "User Not Found")
 * public final class UserNotFoundException extends DomainException {
 *     // ...
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpStatusMapping {

    /**
     * The HTTP status code to return for this exception.
     *
     * @return the HTTP status
     */
    HttpStatus status();

    /**
     * The error type description.
     *
     * @return the error type
     */
    String errorType();
}