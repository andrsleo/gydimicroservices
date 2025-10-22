package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

/**
 * Port for refresh token persistence operations.
 *
 * <p>This interface defines the contract for refresh token storage
 * following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 */
public interface RefreshTokenRepositoryPort {

    /**
     * Saves a refresh token.
     *
     * @param refreshToken the refresh token to save
     * @return the saved refresh token
     */
    RefreshToken save(RefreshToken refreshToken);

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return list of refresh tokens
     */
    List<RefreshToken> findByUserId(Long userId);

    /**
     * Revokes all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    int revokeAllByUserId(Long userId);

    /**
     * Deletes expired refresh tokens.
     */
    void deleteExpiredTokens();
}