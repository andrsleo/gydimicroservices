package com.affiliate.rentals.gydi.users.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a refresh token for authentication.
 *
 * <p>Refresh tokens are long-lived tokens used to obtain new access tokens
 * without requiring the user to re-authenticate. They enhance security by
 * allowing shorter-lived access tokens while maintaining a good user experience.</p>
 *
 * @author GYDI Development Team
 */
public class RefreshToken {

    private Long id;
    private String token;
    private Long userId;
    private Instant expiryDate;
    private Instant createdAt;
    private boolean revoked;

    /**
     * Private constructor for builder pattern.
     */
    private RefreshToken() {
    }

    /**
     * Creates a new refresh token for a user.
     *
     * @param userId the ID of the user
     * @param expirationMillis expiration time in milliseconds from now
     * @return a new RefreshToken instance
     */
    public static RefreshToken create(Long userId, long expirationMillis) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = UUID.randomUUID().toString();
        refreshToken.userId = userId;
        refreshToken.createdAt = Instant.now();
        refreshToken.expiryDate = refreshToken.createdAt.plusMillis(expirationMillis);
        refreshToken.revoked = false;
        return refreshToken;
    }

    /**
     * Reconstitutes a refresh token from persistence.
     *
     * @param id the refresh token ID
     * @param token the token string
     * @param userId the user ID
     * @param expiryDate the expiry date
     * @param createdAt the creation date
     * @param revoked whether the token is revoked
     * @return a RefreshToken instance
     */
    public static RefreshToken reconstitute(
            Long id,
            String token,
            Long userId,
            Instant expiryDate,
            Instant createdAt,
            boolean revoked
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.id = id;
        refreshToken.token = token;
        refreshToken.userId = userId;
        refreshToken.expiryDate = expiryDate;
        refreshToken.createdAt = createdAt;
        refreshToken.revoked = revoked;
        return refreshToken;
    }

    /**
     * Checks if the refresh token is expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Checks if the refresh token is valid (not expired and not revoked).
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Revokes this refresh token.
     */
    public void revoke() {
        this.revoked = true;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}