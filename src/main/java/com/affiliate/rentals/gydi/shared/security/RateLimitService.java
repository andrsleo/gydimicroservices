package com.affiliate.rentals.gydi.shared.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for rate limiting API endpoints using Bucket4j token bucket algorithm.
 *
 * <p>Implements rate limiting per IP address to prevent:
 * <ul>
 *   <li>Brute force attacks on authentication endpoints</li>
 *   <li>Credential stuffing attacks</li>
 *   <li>Account enumeration</li>
 *   <li>DoS attacks</li>
 * </ul>
 *
 * <p><b>Rate Limit Configurations:</b></p>
 * <ul>
 *   <li><b>Authentication endpoints</b> (/login, /register): 5 attempts per 15 minutes per IP</li>
 *   <li><b>Password reset</b>: 3 attempts per 1 hour per IP</li>
 *   <li><b>General API</b>: 100 requests per 1 minute per IP</li>
 * </ul>
 *
 * @author GYDI Security Team
 * @version 1.0
 * @since 2025-11-07
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * Cache of rate limit buckets per client IP address.
     * Key: IP address
     * Value: Bucket for that IP
     */
    private final Map<String, Bucket> authBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalApiBucketCache = new ConcurrentHashMap<>();

    /**
     * Authentication endpoints rate limit: 10 attempts per 15 minutes.
     * Protects against brute force attacks while allowing legitimate users to retry.
     *
     * Note: Each login attempt may consume 1-2 tokens due to NextAuth flow:
     * - Token 1: NextAuth calls /login for validation
     * - Token 2: Frontend may call /login again for full response
     *
     * With 10 tokens, users get ~5-10 real attempts before being rate limited.
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(
            10,  // 10 tokens (5-10 login attempts accounting for double-call pattern)
            Refill.intervally(10, Duration.ofMinutes(15))  // Refill 10 tokens every 15 minutes
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Password reset rate limit: 3 attempts per 1 hour.
     * Prevents password reset abuse.
     */
    private Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(
            3,  // 3 tokens
            Refill.intervally(3, Duration.ofHours(1))  // Refill 3 tokens every hour
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * General API rate limit: 100 requests per 1 minute.
     * Prevents DoS attacks.
     */
    private Bucket createGeneralApiBucket() {
        Bandwidth limit = Bandwidth.classic(
            100,  // 100 tokens
            Refill.intervally(100, Duration.ofMinutes(1))  // Refill 100 tokens every minute
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Resolves the bucket for authentication endpoints based on client IP.
     *
     * @param clientIp the client's IP address
     * @return the bucket for this IP
     */
    private Bucket resolveAuthBucket(String clientIp) {
        return authBucketCache.computeIfAbsent(clientIp, k -> createAuthBucket());
    }

    /**
     * Resolves the bucket for password reset based on client IP.
     *
     * @param clientIp the client's IP address
     * @return the bucket for this IP
     */
    private Bucket resolvePasswordResetBucket(String clientIp) {
        return passwordResetBucketCache.computeIfAbsent(clientIp, k -> createPasswordResetBucket());
    }

    /**
     * Resolves the bucket for general API based on client IP.
     *
     * @param clientIp the client's IP address
     * @return the bucket for this IP
     */
    private Bucket resolveGeneralApiBucket(String clientIp) {
        return generalApiBucketCache.computeIfAbsent(clientIp, k -> createGeneralApiBucket());
    }

    /**
     * Attempts to consume a token from the authentication rate limit bucket.
     *
     * @param request the HTTP request
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsumeAuth(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Bucket bucket = resolveAuthBucket(clientIp);

        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for authentication endpoint. IP: {}", clientIp);
        } else {
            log.debug("Authentication request allowed for IP: {}. Remaining tokens: {}",
                clientIp, bucket.getAvailableTokens());
        }

        return consumed;
    }

    /**
     * Attempts to consume a token from the password reset rate limit bucket.
     *
     * @param request the HTTP request
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsumePasswordReset(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Bucket bucket = resolvePasswordResetBucket(clientIp);

        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for password reset endpoint. IP: {}", clientIp);
        }

        return consumed;
    }

    /**
     * Attempts to consume a token from the general API rate limit bucket.
     *
     * @param request the HTTP request
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsumeGeneralApi(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Bucket bucket = resolveGeneralApiBucket(clientIp);

        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for general API. IP: {}", clientIp);
        }

        return consumed;
    }

    /**
     * Extracts the client's IP address from the request.
     * Checks X-Forwarded-For header first (for proxies/load balancers).
     *
     * @param request the HTTP request
     * @return the client's IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (for requests behind proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
            // Take the first one (original client IP)
            return xForwardedFor.split(",")[0].trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Gets the number of remaining tokens for authentication for a given IP.
     * Useful for informing users how many attempts they have left.
     *
     * @param request the HTTP request
     * @return the number of remaining attempts
     */
    public long getRemainingAuthAttempts(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Bucket bucket = resolveAuthBucket(clientIp);
        return bucket.getAvailableTokens();
    }

    /**
     * Clears the rate limit cache for a specific IP.
     * Useful for administrative actions or testing.
     *
     * @param ipAddress the IP address to clear
     */
    public void clearRateLimitForIp(String ipAddress) {
        authBucketCache.remove(ipAddress);
        passwordResetBucketCache.remove(ipAddress);
        generalApiBucketCache.remove(ipAddress);
        log.info("Cleared rate limit cache for IP: {}", ipAddress);
    }

    /**
     * Clears all rate limit caches.
     * Use with caution - only for maintenance or testing.
     */
    public void clearAllRateLimits() {
        authBucketCache.clear();
        passwordResetBucketCache.clear();
        generalApiBucketCache.clear();
        log.warn("Cleared all rate limit caches");
    }
}
