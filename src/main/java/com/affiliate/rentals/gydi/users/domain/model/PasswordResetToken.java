package com.affiliate.rentals.gydi.users.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * PasswordResetToken domain entity representing a password reset request.
 *
 * <p>This is an immutable domain entity following Domain-Driven Design (DDD) principles.
 * It encapsulates all password reset token data including generation, validation, and usage tracking.
 * The class uses the Builder pattern for flexible object construction and ensures all business
 * rules are enforced through validation.</p>
 *
 * <p>Business Rules:</p>
 * <ul>
 *   <li>Tokens expire after a configurable duration (typically 1 hour)</li>
 *   <li>Tokens can only be used once</li>
 *   <li>Tokens are unique and generated using UUID v4</li>
 *   <li>IP address is tracked for security auditing</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PasswordResetToken token = PasswordResetToken.builder()
 *     .userId(123L)
 *     .expirationMinutes(60)
 *     .ipAddress("192.168.1.1")
 *     .build();
 *
 * if (token.isExpired()) {
 *     throw new TokenExpiredException();
 * }
 *
 * if (token.isUsed()) {
 *     throw new TokenAlreadyUsedException();
 * }
 *
 * // Mark token as used after successful password reset
 * PasswordResetToken usedToken = token.markAsUsed();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see User
 */
public final class PasswordResetToken {
    private final Long id;
    private final Long userId;
    private final String token;
    private final LocalDateTime expiresAt;
    private final boolean used;
    private final LocalDateTime createdAt;
    private final LocalDateTime usedAt;
    private final String ipAddress;

    /**
     * Private constructor used by the Builder.
     * Validates all required fields and creates an immutable PasswordResetToken instance.
     *
     * @param builder the builder instance containing all token data
     * @throws NullPointerException if any required field is null
     */
    private PasswordResetToken(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "User ID cannot be null");
        this.token = builder.token != null ? builder.token : generateSecureToken();
        this.expiresAt = Objects.requireNonNull(builder.expiresAt, "Expiration time cannot be null");
        this.used = builder.used;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
        this.usedAt = builder.usedAt;
        this.ipAddress = builder.ipAddress;

        // Business rule validation
        if (this.expiresAt.isBefore(this.createdAt)) {
            throw new IllegalArgumentException("Expiration time cannot be before creation time");
        }
    }

    /**
     * Generates a cryptographically secure random token using UUID v4.
     *
     * @return a unique token string
     */
    private static String generateSecureToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the token's unique identifier.
     *
     * @return the token ID, may be {@code null} for new tokens not yet persisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the user ID associated with this password reset token.
     *
     * @return the user ID, never {@code null}
     */
    public Long userId() {
        return userId;
    }

    /**
     * Returns the unique token string.
     *
     * @return the token string, never {@code null}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the expiration timestamp of this token.
     *
     * @return the expiration timestamp, never {@code null}
     */
    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    /**
     * Returns whether this token has been used.
     *
     * @return {@code true} if the token has been used, {@code false} otherwise
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * Returns the timestamp when this token was created.
     *
     * @return the creation timestamp, never {@code null}
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp when this token was used.
     *
     * @return the usage timestamp, may be {@code null} if not used
     */
    public LocalDateTime usedAt() {
        return usedAt;
    }

    /**
     * Returns the IP address from which the reset was requested.
     *
     * @return the IP address, may be {@code null}
     */
    public String ipAddress() {
        return ipAddress;
    }

    /**
     * Checks if the token has expired.
     * A token is considered expired if the current time is after the expiration time.
     *
     * @return {@code true} if the token is expired, {@code false} otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the token is valid for use.
     * A token is valid if it has not expired and has not been used.
     *
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isValid() {
        return !isExpired() && !used;
    }

    /**
     * Marks this token as used by creating a new instance with used=true and usedAt=now.
     * This method maintains immutability by returning a new instance.
     *
     * @return a new PasswordResetToken instance marked as used
     * @throws IllegalStateException if the token is already used
     */
    public PasswordResetToken markAsUsed() {
        if (used) {
            throw new IllegalStateException("Token is already marked as used");
        }

        return PasswordResetToken.builder()
                .id(this.id)
                .userId(this.userId)
                .token(this.token)
                .expiresAt(this.expiresAt)
                .used(true)
                .createdAt(this.createdAt)
                .usedAt(LocalDateTime.now())
                .ipAddress(this.ipAddress)
                .build();
    }

    /**
     * Validates the token for password reset operation.
     * Throws specific exceptions for different invalid states.
     *
     * @throws IllegalStateException if the token is expired or already used
     */
    public void validateForPasswordReset() {
        if (isExpired()) {
            throw new IllegalStateException("Password reset token has expired");
        }
        if (used) {
            throw new IllegalStateException("Password reset token has already been used");
        }
    }

    /**
     * Creates a new Builder instance for constructing PasswordResetToken objects.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return Objects.equals(id, that.id) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token);
    }

    @Override
    public String toString() {
        return "PasswordResetToken[id=%d, userId=%d, expired=%b, used=%b, createdAt=%s]"
                .formatted(id, userId, isExpired(), used, createdAt);
    }

    /**
     * Builder class for constructing {@link PasswordResetToken} instances.
     *
     * <p>This builder follows the fluent interface pattern and provides
     * methods for setting all token properties. It validates required fields
     * when {@link #build()} is called.</p>
     */
    public static final class Builder {
        private Long id;
        private Long userId;
        private String token;
        private LocalDateTime expiresAt;
        private boolean used;
        private LocalDateTime createdAt;
        private LocalDateTime usedAt;
        private String ipAddress;

        private Builder() {
        }

        /**
         * Sets the token ID.
         *
         * @param id the token ID
         * @return this builder instance
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the user ID associated with this token.
         *
         * @param userId the user ID
         * @return this builder instance
         */
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the token string.
         * If not set, a random UUID will be generated automatically.
         *
         * @param token the token string
         * @return this builder instance
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the expiration timestamp.
         *
         * @param expiresAt the expiration timestamp
         * @return this builder instance
         */
        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * Sets the expiration time based on minutes from now.
         * Convenience method to set expiration relative to current time.
         *
         * @param minutes the number of minutes until expiration
         * @return this builder instance
         */
        public Builder expirationMinutes(int minutes) {
            this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
            return this;
        }

        /**
         * Sets whether the token has been used.
         *
         * @param used the usage status
         * @return this builder instance
         */
        public Builder used(boolean used) {
            this.used = used;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the usage timestamp.
         *
         * @param usedAt the usage timestamp
         * @return this builder instance
         */
        public Builder usedAt(LocalDateTime usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        /**
         * Sets the IP address from which the reset was requested.
         *
         * @param ipAddress the IP address
         * @return this builder instance
         */
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * Builds and returns a new {@link PasswordResetToken} instance.
         *
         * @return the constructed PasswordResetToken
         * @throws NullPointerException if any required field is null
         * @throws IllegalArgumentException if validation fails
         */
        public PasswordResetToken build() {
            return new PasswordResetToken(this);
        }
    }
}