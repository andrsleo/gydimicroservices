package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.TokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Spring Data JPA repository for TokenBlacklistEntity.
 *
 * @author GYDI Development Team
 */
@Repository
public interface TokenBlacklistJpaRepository extends JpaRepository<TokenBlacklistEntity, Long> {

    /**
     * Checks if a token is blacklisted.
     *
     * @param token the token string
     * @return true if blacklisted, false otherwise
     */
    boolean existsByToken(String token);

    /**
     * Deletes all expired blacklisted tokens.
     *
     * @param now current timestamp
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklistEntity t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}