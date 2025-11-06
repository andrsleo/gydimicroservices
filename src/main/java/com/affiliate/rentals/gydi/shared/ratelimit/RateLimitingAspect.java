package com.affiliate.rentals.gydi.shared.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect for enforcing rate limiting on annotated methods.
 *
 * <p>This aspect intercepts methods annotated with {@link RateLimited} and enforces
 * request rate limits using the Token Bucket algorithm (via Bucket4j library).</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Per-IP address rate limiting</li>
 *   <li>Configurable request limits and time windows</li>
 *   <li>In-memory bucket storage (suitable for single instance deployments)</li>
 *   <li>Automatic bucket cleanup to prevent memory leaks</li>
 *   <li>Comprehensive logging for security monitoring</li>
 * </ul>
 *
 * <p>Rate limiting strategy:</p>
 * <ul>
 *   <li>Uses Token Bucket algorithm</li>
 *   <li>Refills tokens gradually over the time window</li>
 *   <li>Each request consumes one token</li>
 *   <li>Rejects requests when bucket is empty</li>
 * </ul>
 *
 * <p>Note: For distributed deployments, consider using a shared cache (Redis)
 * instead of the in-memory ConcurrentHashMap.</p>
 *
 * @author GYDI Development Team
 * @see RateLimited
 */
@Aspect
@Component
public class RateLimitingAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingAspect.class);

    // In-memory storage of buckets per IP address
    // Key format: "methodName:ipAddress"
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * Around advice that enforces rate limiting on methods annotated with @RateLimited.
     *
     * @param joinPoint the join point representing the intercepted method
     * @param rateLimited the rate limit annotation
     * @return the result of the method execution
     * @throws Throwable if the method execution fails or rate limit is exceeded
     */
    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();

        if (request == null) {
            log.warn("Could not obtain HttpServletRequest, skipping rate limiting");
            return joinPoint.proceed();
        }

        String ipAddress = extractIpAddress(request);
        String methodName = extractMethodName(joinPoint);
        String bucketKey = methodName + ":" + ipAddress;

        // Get or create bucket for this IP and method
        Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k ->
                createBucket(rateLimited.requests(), rateLimited.windowHours())
        );

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            log.debug("Rate limit check passed for {} from IP {}", methodName, ipAddress);
            return joinPoint.proceed();
        } else {
            log.warn("Rate limit exceeded for {} from IP {}. Limit: {} requests per {} hour(s)",
                    methodName, ipAddress, rateLimited.requests(), rateLimited.windowHours());

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    rateLimited.message()
            );
        }
    }

    /**
     * Creates a new Bucket4j bucket with the specified capacity and refill rate.
     *
     * @param capacity the maximum number of tokens (requests allowed)
     * @param windowHours the time window in hours
     * @return a new Bucket instance
     */
    private Bucket createBucket(int capacity, int windowHours) {
        Duration windowDuration = Duration.ofHours(windowHours);

        // Create bandwidth with token bucket algorithm
        // Refill gradually: capacity tokens over the window duration
        Bandwidth bandwidth = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, windowDuration)
        );

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Extracts the client's IP address from the HTTP request.
     * Checks X-Forwarded-For header first (for proxy scenarios).
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // If X-Forwarded-For contains multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Extracts the method name from the join point.
     *
     * @param joinPoint the join point
     * @return the method name
     */
    private String extractMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getName();
    }

    /**
     * Gets the current HTTP request from the request context.
     *
     * @return the HttpServletRequest, or null if not available
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Clears the bucket cache. Useful for testing or administrative operations.
     */
    public void clearCache() {
        log.info("Clearing rate limit bucket cache. Current size: {}", bucketCache.size());
        bucketCache.clear();
    }

    /**
     * Gets the current size of the bucket cache.
     *
     * @return the number of cached buckets
     */
    public int getCacheSize() {
        return bucketCache.size();
    }
}
