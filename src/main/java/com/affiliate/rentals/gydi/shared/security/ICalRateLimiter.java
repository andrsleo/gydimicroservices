package com.affiliate.rentals.gydi.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔒 Rate limiter for iCal synchronization
 *
 * Prevents:
 * - User abuse (too many properties)
 * - IP blocking by external services (Airbnb, Booking.com)
 * - Resource exhaustion
 *
 * Limits:
 * - Per user: 100 syncs/hour
 * - Per property: 1 sync/hour (calendar data doesn't change often)
 * - Global: 1000 syncs/hour (prevent IP blocking)
 */
@Slf4j
@Service
public class ICalRateLimiter {

    // ✅ Rate limit configuration
    private static final int MAX_SYNCS_PER_USER_PER_HOUR = 100;
    private static final int MAX_SYNCS_PER_PROPERTY_PER_HOUR = 1;
    private static final int MAX_GLOBAL_SYNCS_PER_HOUR = 1000;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    // ✅ In-memory storage (TODO: move to Redis for distributed systems)
    private final Map<String, RateLimitBucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<Long, RateLimitBucket> propertyBuckets = new ConcurrentHashMap<>();
    private final RateLimitBucket globalBucket = new RateLimitBucket(MAX_GLOBAL_SYNCS_PER_HOUR);

    /**
     * Checks if sync is allowed for given user and property
     *
     * @param userId User ID
     * @param propertyId Property ID
     * @throws RateLimitExceededException if rate limit exceeded
     */
    public void checkRateLimit(Long userId, Long propertyId) {
        String userKey = "user:" + userId;

        // ✅ Check global rate limit first
        if (!globalBucket.tryConsume()) {
            log.warn("🚨 Global rate limit exceeded. Syncs/hour: {}", globalBucket.getRequestCount());
            throw new RateLimitExceededException(
                "System is currently busy. Please try again in " +
                globalBucket.getTimeUntilReset().toMinutes() + " minutes"
            );
        }

        // ✅ Check per-user rate limit
        RateLimitBucket userBucket = userBuckets.computeIfAbsent(
            userKey,
            k -> new RateLimitBucket(MAX_SYNCS_PER_USER_PER_HOUR)
        );

        if (!userBucket.tryConsume()) {
            log.warn("🚨 User {} exceeded rate limit. Syncs/hour: {}", userId, userBucket.getRequestCount());
            throw new RateLimitExceededException(
                "You have exceeded the sync limit (" + MAX_SYNCS_PER_USER_PER_HOUR + " per hour). " +
                "Please try again in " + userBucket.getTimeUntilReset().toMinutes() + " minutes"
            );
        }

        // ✅ Check per-property rate limit (prevent hammering same property)
        RateLimitBucket propertyBucket = propertyBuckets.computeIfAbsent(
            propertyId,
            k -> new RateLimitBucket(MAX_SYNCS_PER_PROPERTY_PER_HOUR)
        );

        if (!propertyBucket.tryConsume()) {
            log.warn("🚨 Property {} exceeded rate limit. Last sync: {} minutes ago",
                propertyId,
                Duration.between(propertyBucket.getLastReset(), Instant.now()).toMinutes()
            );
            throw new RateLimitExceededException(
                "This property was recently synced. " +
                "Please wait " + propertyBucket.getTimeUntilReset().toMinutes() + " minutes before syncing again"
            );
        }

        log.debug("Rate limit check passed for user {} and property {}", userId, propertyId);
    }

    /**
     * Gets current rate limit status for user
     */
    public RateLimitStatus getUserRateLimitStatus(Long userId) {
        String userKey = "user:" + userId;
        RateLimitBucket bucket = userBuckets.get(userKey);

        if (bucket == null) {
            return new RateLimitStatus(
                MAX_SYNCS_PER_USER_PER_HOUR,
                MAX_SYNCS_PER_USER_PER_HOUR,
                Duration.ZERO
            );
        }

        return new RateLimitStatus(
            MAX_SYNCS_PER_USER_PER_HOUR,
            bucket.getRemainingRequests(),
            bucket.getTimeUntilReset()
        );
    }

    /**
     * Cleans up expired buckets (call periodically)
     */
    public void cleanupExpiredBuckets() {
        Instant now = Instant.now();

        userBuckets.entrySet().removeIf(entry ->
            Duration.between(entry.getValue().getLastReset(), now).compareTo(RATE_LIMIT_WINDOW) > 0
        );

        propertyBuckets.entrySet().removeIf(entry ->
            Duration.between(entry.getValue().getLastReset(), now).compareTo(RATE_LIMIT_WINDOW) > 0
        );

        log.debug("Cleaned up expired rate limit buckets");
    }

    /**
     * Token bucket for rate limiting
     */
    private static class RateLimitBucket {
        private final int maxRequests;
        private int remainingRequests;
        private Instant lastReset;

        public RateLimitBucket(int maxRequests) {
            this.maxRequests = maxRequests;
            this.remainingRequests = maxRequests;
            this.lastReset = Instant.now();
        }

        public synchronized boolean tryConsume() {
            resetIfNeeded();

            if (remainingRequests > 0) {
                remainingRequests--;
                return true;
            }

            return false;
        }

        private void resetIfNeeded() {
            Instant now = Instant.now();
            if (Duration.between(lastReset, now).compareTo(RATE_LIMIT_WINDOW) >= 0) {
                remainingRequests = maxRequests;
                lastReset = now;
            }
        }

        public int getRemainingRequests() {
            resetIfNeeded();
            return remainingRequests;
        }

        public int getRequestCount() {
            return maxRequests - remainingRequests;
        }

        public Duration getTimeUntilReset() {
            Instant resetTime = lastReset.plus(RATE_LIMIT_WINDOW);
            return Duration.between(Instant.now(), resetTime);
        }

        public Instant getLastReset() {
            return lastReset;
        }
    }

    /**
     * Rate limit status DTO
     */
    public record RateLimitStatus(
        int limit,
        int remaining,
        Duration resetIn
    ) {}

    /**
     * Custom exception for rate limit exceeded
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
