package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RefreshTokenEntity with rotation support.
 *
 * @author GYDI Development Team
 */
@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the refresh token if found
     */
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * Finds all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return list of refresh tokens
     */
    List<RefreshTokenEntity> findByUserId(Long userId);

    /**
     * Finds all refresh tokens in a token family.
     *
     * @param tokenFamilyId the token family ID
     * @return list of refresh tokens in the family
     */
    List<RefreshTokenEntity> findByTokenFamilyId(UUID tokenFamilyId);

    /**
     * Revokes all refresh tokens for a user by setting revokedAt timestamp.
     *
     * <p>Using @Modifying with clearAutomatically to ensure the persistence context
     * is cleared after the update, following Spring Data JPA best practices.</p>
     *
     * @param userId the user ID
     * @param revokedAt the timestamp to set
     * @return the number of tokens revoked
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :revokedAt WHERE r.userId = :userId AND r.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    /**
     * Revokes all refresh tokens in a token family by setting revokedAt timestamp.
     *
     * <p>This method is critical for security - it's invoked when token reuse is detected,
     * revoking all tokens in the rotation chain to prevent attackers from using stolen tokens.</p>
     *
     * @param tokenFamilyId the token family ID
     * @param revokedAt the timestamp to set
     * @return the number of tokens revoked
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :revokedAt WHERE r.tokenFamilyId = :familyId AND r.revokedAt IS NULL")
    int revokeAllByTokenFamily(@Param("familyId") UUID tokenFamilyId, @Param("revokedAt") Instant revokedAt);

    /**
     * Deletes all expired refresh tokens.
     *
     * @param now current timestamp
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}