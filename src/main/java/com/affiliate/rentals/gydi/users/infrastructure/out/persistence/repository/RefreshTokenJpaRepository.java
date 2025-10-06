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

/**
 * Spring Data JPA repository for RefreshTokenEntity.
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
     * Revokes all refresh tokens for a user by setting revoked flag to true.
     *
     * <p>Using @Modifying with clearAutomatically to ensure the persistence context
     * is cleared after the update, following Spring Data JPA best practices.</p>
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    /**
     * Deletes all expired refresh tokens.
     *
     * @param now current timestamp
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}