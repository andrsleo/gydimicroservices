package com.affiliate.rentals.gydi.shared.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be rate limited.
 *
 * <p>This annotation is processed by {@link RateLimitingAspect} to enforce
 * request limits on API endpoints. It's typically used for sensitive operations
 * like password reset, login attempts, and registration.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @PostMapping("/api/v1/auth/forgot-password")
 * @RateLimited(requests = 3, windowHours = 1)
 * public ResponseEntity<PasswordResetResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
 *     // method implementation
 * }
 * }</pre>
 *
 * <p>Rate limiting is applied per IP address by default.</p>
 *
 * @author GYDI Development Team
 * @see RateLimitingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Maximum number of requests allowed within the time window.
     *
     * @return the request limit
     */
    int requests() default 10;

    /**
     * Time window in hours for the rate limit.
     *
     * @return the time window in hours
     */
    int windowHours() default 1;

    /**
     * Custom error message to return when rate limit is exceeded.
     * If not specified, a default message will be used.
     *
     * @return the error message
     */
    String message() default "Too many requests. Please try again later.";
}
