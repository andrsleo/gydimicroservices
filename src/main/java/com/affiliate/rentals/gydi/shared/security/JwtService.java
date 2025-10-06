package com.affiliate.rentals.gydi.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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
     */
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
     * Gets the signing key for JWT operations.
     *
     * @return the secret key
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
