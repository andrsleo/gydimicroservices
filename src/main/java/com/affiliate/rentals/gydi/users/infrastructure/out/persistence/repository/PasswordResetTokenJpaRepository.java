package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PasswordResetTokenEntity.
 *
 * <p>This repository provides database access operations for password reset tokens.
 * It extends JpaRepository to inherit standard CRUD operations and defines custom
 * queries for password reset-specific operations.</p>
 *
 * <p>This is an infrastructure-layer interface following hexagonal architecture principles.
 * The actual implementation is provided automatically by Spring Data JPA.</p>
 *
 * @author GYDI Development Team
 * @see PasswordResetTokenEntity
 */
@Repository
public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the token if found, empty otherwise
     */
    Optional<PasswordResetTokenEntity> findByToken(String token);

    /**
     * Finds the most recent valid (non-used, non-expired) token for a user.
     *
     * @param userId the user ID
     * @param currentTime the current timestamp for expiration checking
     * @return an Optional containing the latest valid token if found, empty otherwise
     */
    @Query("""
            SELECT t FROM PasswordResetTokenEntity t
            WHERE t.userId = :userId
              AND t.used = false
              AND t.expiresAt > :currentTime
            ORDER BY t.createdAt DESC
            LIMIT 1
            """)
    Optional<PasswordResetTokenEntity> findLatestValidTokenByUserId(
            @Param("userId") Long userId,
            @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Finds all tokens for a specific user, ordered by creation date descending.
     *
     * @param userId the user ID
     * @return list of tokens for the user
     */
    List<PasswordResetTokenEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Marks all unused tokens for a user as used.
     * This is called when a user successfully resets their password or requests a new token.
     *
     * @param userId the user ID
     * @return the number of tokens updated
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetTokenEntity t
            SET t.used = true, t.usedAt = :usedAt
            WHERE t.userId = :userId AND t.used = false
            """)
    int invalidateAllTokensForUser(
            @Param("userId") Long userId,
            @Param("usedAt") LocalDateTime usedAt
    );

    /**
     * Deletes all expired tokens.
     *
     * @param currentTime the current timestamp
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("""
            DELETE FROM PasswordResetTokenEntity t
            WHERE t.expiresAt < :currentTime
            """)
    int deleteByExpiresAtBefore(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Deletes all used tokens that were marked as used before the specified timestamp.
     *
     * @param cutoffTime the cutoff timestamp
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("""
            DELETE FROM PasswordResetTokenEntity t
            WHERE t.used = true AND t.usedAt < :cutoffTime
            """)
    int deleteByUsedTrueAndUsedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Checks if a user has any valid (non-expired, non-used) tokens.
     *
     * @param userId the user ID
     * @param currentTime the current timestamp
     * @return true if at least one valid token exists, false otherwise
     */
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM PasswordResetTokenEntity t
            WHERE t.userId = :userId
              AND t.used = false
              AND t.expiresAt > :currentTime
            """)
    boolean hasValidToken(
            @Param("userId") Long userId,
            @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Counts the number of password reset requests for a user within a time window.
     *
     * @param userId the user ID
     * @param since the start of the time window
     * @return the count of requests
     */
    @Query("""
            SELECT COUNT(t)
            FROM PasswordResetTokenEntity t
            WHERE t.userId = :userId
              AND t.createdAt >= :since
            """)
    int countRequestsSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}
