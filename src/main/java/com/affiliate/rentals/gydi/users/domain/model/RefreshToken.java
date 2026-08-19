package com.affiliate.rentals.gydi.users.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a refresh token for authentication with rotation support.
 *
 * <p>Refresh tokens are long-lived tokens used to obtain new access tokens
 * without requiring the user to re-authenticate. This implementation follows
 * OWASP best practices by enforcing one-time use with automatic rotation.</p>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li><b>One-Time Use:</b> Each token can only be used once for refresh</li>
 *   <li><b>Token Families:</b> All rotated tokens share the same family ID</li>
 *   <li><b>Reuse Detection:</b> Attempting to reuse a token revokes the entire family</li>
 *   <li><b>Automatic Rotation:</b> New token issued on each refresh operation</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public class RefreshToken {

    private Long id;
    private String token;
    private Long userId;
    private UUID tokenFamilyId;
    private Instant expiryDate;
    private Instant createdAt;
    private Instant usedAt;
    private Instant revokedAt;
    private String replacedByToken;

    /**
     * Private constructor for builder pattern.
     */
    private RefreshToken() {
    }

    /**
     * Creates a new refresh token for a user, starting a new token family.
     *
     * @param userId the ID of the user
     * @param expirationMillis expiration time in milliseconds from now
     * @return a new RefreshToken instance with a new token family
     */
    public static RefreshToken create(Long userId, long expirationMillis) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = UUID.randomUUID().toString();
        refreshToken.userId = userId;
        refreshToken.tokenFamilyId = UUID.randomUUID(); // New family
        refreshToken.createdAt = Instant.now();
        refreshToken.expiryDate = refreshToken.createdAt.plusMillis(expirationMillis);
        refreshToken.usedAt = null;
        refreshToken.revokedAt = null;
        refreshToken.replacedByToken = null;
        return refreshToken;
    }

    /**
     * Creates a rotated refresh token within the same token family.
     *
     * <p>This method creates a new token that belongs to the same family as the original,
     * allowing detection of token reuse attacks.</p>
     *
     * @param userId the ID of the user
     * @param tokenFamilyId the family ID to continue
     * @param expirationMillis expiration time in milliseconds from now
     * @return a new RefreshToken instance in the same family
     */
    public static RefreshToken createRotated(Long userId, UUID tokenFamilyId, long expirationMillis) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = UUID.randomUUID().toString();
        refreshToken.userId = userId;
        refreshToken.tokenFamilyId = tokenFamilyId; // Same family
        refreshToken.createdAt = Instant.now();
        refreshToken.expiryDate = refreshToken.createdAt.plusMillis(expirationMillis);
        refreshToken.usedAt = null;
        refreshToken.revokedAt = null;
        refreshToken.replacedByToken = null;
        return refreshToken;
    }

    /**
     * Reconstitutes a refresh token from persistence.
     *
     * @param id the refresh token ID
     * @param token the token string
     * @param userId the user ID
     * @param tokenFamilyId the token family ID
     * @param expiryDate the expiry date
     * @param createdAt the creation date
     * @param usedAt when the token was used (null if unused)
     * @param revokedAt when the token was revoked (null if not revoked)
     * @param replacedByToken the token that replaced this one
     * @return a RefreshToken instance
     */
    public static RefreshToken reconstitute(
            Long id,
            String token,
            Long userId,
            UUID tokenFamilyId,
            Instant expiryDate,
            Instant createdAt,
            Instant usedAt,
            Instant revokedAt,
            String replacedByToken
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.id = id;
        refreshToken.token = token;
        refreshToken.userId = userId;
        refreshToken.tokenFamilyId = tokenFamilyId;
        refreshToken.expiryDate = expiryDate;
        refreshToken.createdAt = createdAt;
        refreshToken.usedAt = usedAt;
        refreshToken.revokedAt = revokedAt;
        refreshToken.replacedByToken = replacedByToken;
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
     * Checks if the token has been used (one-time use enforcement).
     *
     * @return true if the token has been used, false otherwise
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if the token is revoked.
     *
     * @return true if the token is revoked, false otherwise
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks if the refresh token is valid (not expired, not used, and not revoked).
     *
     * <p>A valid token meets all of these conditions:</p>
     * <ul>
     *   <li>Not expired</li>
     *   <li>Never been used (one-time use)</li>
     *   <li>Not revoked</li>
     * </ul>
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !isUsed() && !isRevoked();
    }

    /**
     * Marks this refresh token as used during rotation.
     *
     * <p>This enforces one-time use by recording when the token was consumed
     * and which new token replaced it.</p>
     *
     * @param newToken the new token that replaced this one
     */
    public void markAsUsed(String newToken) {
        this.usedAt = Instant.now();
        this.replacedByToken = newToken;
    }

    /**
     * Revokes this refresh token.
     *
     * <p>This is typically used when detecting token reuse attacks
     * or during explicit logout operations.</p>
     */
    public void revoke() {
        this.revokedAt = Instant.now();
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

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getReplacedByToken() {
        return replacedByToken;
    }
}