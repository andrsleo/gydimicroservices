package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity representing a blacklisted JWT token.
 *
 * <p>This entity maps to the {@code token_blacklist} table in the {@code users} schema.
 * Blacklisted tokens are access tokens that have been invalidated through logout.</p>
 *
 * @author GYDI Development Team
 */
@Entity
@Table(name = "token_blacklist", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    private Instant blacklistedAt;

    @PrePersist
    protected void onCreate() {
        if (blacklistedAt == null) {
            blacklistedAt = Instant.now();
        }
    }
}