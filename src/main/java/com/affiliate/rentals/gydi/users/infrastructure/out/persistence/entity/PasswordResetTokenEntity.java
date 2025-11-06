package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a password reset token in the users schema.
 *
 * <p>This entity maps to the {@code password_reset_tokens} table in the {@code users} schema.
 * It stores password reset tokens with expiration and usage tracking information.</p>
 *
 * <p>This is an infrastructure-layer class that implements the persistence
 * details for the PasswordResetToken domain model, following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 * @see UserEntity
 */
@Entity
@Table(
        name = "password_reset_tokens",
        schema = "users",
        indexes = {
                @Index(name = "idx_password_reset_tokens_token", columnList = "token"),
                @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_password_reset_tokens_expires_at", columnList = "expires_at"),
                @Index(name = "idx_password_reset_tokens_used", columnList = "used")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenEntity {

    /**
     * The unique identifier for this token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user associated with this reset token.
     * Cannot be null.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The unique token string (UUID).
     * Cannot be null and must be unique.
     */
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    /**
     * The timestamp when this token expires.
     * Cannot be null.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Flag indicating if this token has been used.
     * Defaults to false.
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    /**
     * The timestamp when this token was created.
     * Defaults to current timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The timestamp when this token was used.
     * Null if not used.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * The IP address from which the reset was requested.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * JPA lifecycle callback to set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetTokenEntity that = (PasswordResetTokenEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token);
    }

    @Override
    public String toString() {
        return "PasswordResetTokenEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", expired=" + (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) +
                ", used=" + used +
                ", createdAt=" + createdAt +
                '}';
    }
}
