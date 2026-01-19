package com.affiliate.rentals.gydi.users.domain.exception;

/**
 * Exception thrown when a user exceeds the allowed rate limit for authentication attempts.
 *
 * <p>This exception is thrown during login operations when a user makes too many
 * authentication attempts within a specified time window. It's part of the security
 * measures to prevent brute force attacks.</p>
 *
 * <p>This exception extends {@link AuthenticationException} and is handled by the
 * GlobalExceptionHandler to return HTTP 429 (Too Many Requests) with appropriate
 * retry-after headers.</p>
 *
 * @author GYDI Development Team
 */
public class RateLimitExceededException extends AuthenticationException {

    private final long remainingAttempts;
    private final long retryAfterSeconds;

    /**
     * Constructs a new RateLimitExceededException with details about remaining attempts.
     *
     * @param remainingAttempts the number of remaining attempts before complete lockout
     * @param retryAfterSeconds the number of seconds to wait before retrying
     */
    public RateLimitExceededException(long remainingAttempts, long retryAfterSeconds) {
        super(String.format(
            "Too many login attempts. Please try again in %d minutes. Remaining attempts: %d",
            retryAfterSeconds / 60,
            remainingAttempts
        ));
        this.remainingAttempts = remainingAttempts;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Gets the number of remaining attempts before complete lockout.
     *
     * @return the remaining attempts count
     */
    public long getRemainingAttempts() {
        return remainingAttempts;
    }

    /**
     * Gets the number of seconds to wait before retrying.
     *
     * @return the retry-after duration in seconds
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
