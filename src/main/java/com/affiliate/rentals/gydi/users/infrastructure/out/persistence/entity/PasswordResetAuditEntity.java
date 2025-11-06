package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity representing a password reset audit entry in the users schema.
 *
 * <p>This entity maps to the {@code password_reset_audit} table in the {@code users} schema.
 * It stores audit logs for all password reset operations for security tracking and analysis.</p>
 *
 * <p>Audit actions include:</p>
 * <ul>
 *   <li>REQUEST - Password reset was requested</li>
 *   <li>VALIDATE - Token validation was performed</li>
 *   <li>RESET - Password was successfully reset</li>
 *   <li>EXPIRED - Attempt with expired token</li>
 *   <li>INVALID - Attempt with invalid token</li>
 *   <li>RATE_LIMIT_EXCEEDED - Request blocked by rate limiting</li>
 * </ul>
 *
 * <p>This is an infrastructure-layer class following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(
        name = "password_reset_audit",
        schema = "users",
        indexes = {
                @Index(name = "idx_password_reset_audit_user_email", columnList = "user_email"),
                @Index(name = "idx_password_reset_audit_action", columnList = "action"),
                @Index(name = "idx_password_reset_audit_created_at", columnList = "created_at"),
                @Index(name = "idx_password_reset_audit_email_created", columnList = "user_email, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetAuditEntity {

    /**
     * The unique identifier for this audit entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The email address associated with the password reset action.
     * Cannot be null.
     */
    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    /**
     * The type of action performed.
     * Valid values: REQUEST, VALIDATE, RESET, EXPIRED, INVALID, RATE_LIMIT_EXCEEDED
     * Cannot be null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private PasswordResetAction action;

    /**
     * Whether the action was successful.
     * Cannot be null.
     */
    @Column(name = "success", nullable = false)
    private Boolean success = false;

    /**
     * The IP address from which the action was performed.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * The browser/client user agent string.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * The timestamp when this audit entry was created.
     * Defaults to current timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
        PasswordResetAuditEntity that = (PasswordResetAuditEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PasswordResetAuditEntity{" +
                "id=" + id +
                ", userEmail='" + userEmail + '\'' +
                ", action=" + action +
                ", success=" + success +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Enum representing the types of password reset actions that can be audited.
     */
    public enum PasswordResetAction {
        REQUEST,
        VALIDATE,
        RESET,
        EXPIRED,
        INVALID,
        RATE_LIMIT_EXCEEDED
    }
}
