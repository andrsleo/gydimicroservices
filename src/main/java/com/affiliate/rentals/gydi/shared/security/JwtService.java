package com.affiliate.rentals.gydi.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.users.domain.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 *
 * <p>This service handles all JWT operations including token generation,
 * validation, and claims extraction using the JJWT library.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String USER_ID_CLAIM = "userId";
    private static final String SUBSCRIPTION_PLAN_CLAIM = "subscriptionPlan";
    private static final String CAN_PUBLISH_CLAIM = "canPublish";
    private static final String CAN_REFER_CLAIM = "canRefer";
    private static final String CAN_RENT_CLAIM = "canRent";
    private static final String ACCOUNT_VERIFIED_CLAIM = "accountVerified";

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the user ID, or null if not present
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userIdObj = claims.get(USER_ID_CLAIM);
            if (userIdObj == null) {
                return null;
            }
            // Handle both Integer and Long types
            if (userIdObj instanceof Integer intValue) {
                return intValue.longValue();
            }
            if (userIdObj instanceof Long longValue) {
                return longValue;
            }
            // Fallback: try to parse as string
            return Long.parseLong(userIdObj.toString());
        });
    }

    /**
     * Extracts the subscription plan from a JWT token.
     *
     * @param token the JWT token
     * @return the subscription plan name, or null if not present
     */
    public String extractSubscriptionPlan(String token) {
        return extractClaim(token, claims -> (String) claims.get(SUBSCRIPTION_PLAN_CLAIM));
    }

    /**
     * Extracts the canPublish capability from a JWT token.
     *
     * @param token the JWT token
     * @return true if user can publish, false otherwise
     */
    public Boolean extractCanPublish(String token) {
        return extractClaim(token, claims -> (Boolean) claims.get(CAN_PUBLISH_CLAIM));
    }

    /**
     * Extracts the canRefer capability from a JWT token.
     *
     * @param token the JWT token
     * @return true if user can refer, false otherwise
     */
    public Boolean extractCanRefer(String token) {
        return extractClaim(token, claims -> (Boolean) claims.get(CAN_REFER_CLAIM));
    }

    /**
     * Extracts the canRent capability from a JWT token.
     *
     * @param token the JWT token
     * @return true if user can rent, false otherwise
     */
    public Boolean extractCanRent(String token) {
        return extractClaim(token, claims -> (Boolean) claims.get(CAN_RENT_CLAIM));
    }

    /**
     * Extracts the account verification status from a JWT token.
     *
     * @param token the JWT token
     * @return true if account is verified, false otherwise
     */
    public Boolean extractAccountVerified(String token) {
        return extractClaim(token, claims -> (Boolean) claims.get(ACCOUNT_VERIFIED_CLAIM));
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * @param token the JWT token
     * @param claimsResolver function to extract the desired claim
     * @param <T> the type of the claim
     * @return the claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for a user.
     *
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token with additional claims.
     *
     * @param extraClaims additional claims to include
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Generates a JWT token with user ID included in claims.
     *
     * <p>This method follows Java 21 best practices by using type-safe parameters
     * and ensuring immutability of the claims map.</p>
     *
     * @param userDetails the user details
     * @param userId the user ID to include in the token claims
     * @return the generated JWT token with userId claim
     * @deprecated Use {@link #generateTokenWithUserData(UserDetails, User)} instead
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public String generateTokenWithUserId(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, userId);

        // Include user roles/authorities in JWT claims
        var authorities = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        claims.put("roles", authorities);

        return buildToken(claims, userDetails, jwtExpiration);
    }

    /**
     * Generates a JWT token with complete user data including permissions and capabilities.
     *
     * <p>This method creates a comprehensive JWT token that includes:</p>
     * <ul>
     *   <li>User ID and roles</li>
     *   <li>Subscription plan (FREE, PRO, ELITE)</li>
     *   <li>User capabilities (canPublish, canRefer, canRent)</li>
     *   <li>Account verification status</li>
     * </ul>
     *
     * <p>This allows the frontend to make permission decisions without additional API calls,
     * while still validating critical operations on the backend.</p>
     *
     * @param userDetails the Spring Security user details
     * @param user the domain user model with complete information
     * @return the generated JWT token with all user data claims
     */
    public String generateTokenWithUserData(UserDetails userDetails, User user) {
        Map<String, Object> claims = new HashMap<>();

        // Basic user information
        claims.put(USER_ID_CLAIM, user.id());

        // Include user roles/authorities in JWT claims
        var authorities = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        claims.put("roles", authorities);

        // Subscription and permissions information
        claims.put(SUBSCRIPTION_PLAN_CLAIM, user.activePlan().name());
        claims.put(CAN_PUBLISH_CLAIM, user.capabilities().canPublish());
        claims.put(CAN_REFER_CLAIM, user.capabilities().canRefer());
        claims.put(CAN_RENT_CLAIM, user.capabilities().canRent());
        claims.put(ACCOUNT_VERIFIED_CLAIM, user.isAccountVerified());

        return buildToken(claims, userDetails, jwtExpiration);
    }

    /**
     * Builds a JWT token with specified expiration.
     *
     * @param extraClaims additional claims to include
     * @param userDetails the user details
     * @param expiration expiration time in milliseconds
     * @return the generated JWT token
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration, ChronoUnit.MILLIS)))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates a JWT token against user details.
     *
     * @param token the JWT token
     * @param userDetails the user details to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token the JWT token
     * @return the claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the refresh token expiration time in milliseconds.
     *
     * @return the refresh token expiration in milliseconds
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Extracts the user ID from the JWT token in the current request.
     *
     * @param request the HTTP servlet request
     * @return the user ID from the JWT token
     * @throws IllegalStateException if no valid JWT token is found
     */
    public Long extractUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new IllegalStateException("No JWT token found in request");
        }
        Long userId = extractUserId(token);
        if (userId == null) {
            throw new IllegalStateException("No userId claim found in JWT token");
        }
        return userId;
    }

    /**
     * Extracts the JWT token from the request (Authorization header or cookie).
     *
     * @param request the HTTP servlet request
     * @return the JWT token, or null if not found
     */
    private String extractTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        // Try cookie first
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Gets the signing key for JWT operations.
     *
     * @return the secret key
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
