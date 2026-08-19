package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for refresh token persistence operations with rotation support.
 *
 * <p>This interface defines the contract for refresh token storage
 * following hexagonal architecture principles and OWASP security best practices.</p>
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
     * Finds all refresh tokens in a token family.
     *
     * <p>Used for detecting token reuse attacks by examining the entire
     * rotation chain.</p>
     *
     * @param tokenFamilyId the token family ID
     * @return list of refresh tokens in the family
     */
    List<RefreshToken> findByTokenFamilyId(UUID tokenFamilyId);

    /**
     * Revokes all refresh tokens for a user.
     *
     * <p>This is typically used during logout all devices operation.</p>
     *
     * @param userId the user ID
     * @return the number of tokens revoked
     */
    int revokeAllByUserId(Long userId);

    /**
     * Revokes all refresh tokens in a token family.
     *
     * <p>This critical security operation is invoked when token reuse is detected.
     * It immediately invalidates all tokens in the rotation chain, forcing the
     * user to re-authenticate.</p>
     *
     * @param tokenFamilyId the token family ID
     * @return the number of tokens revoked
     */
    int revokeAllByTokenFamily(UUID tokenFamilyId);

    /**
     * Deletes expired refresh tokens.
     */
    void deleteExpiredTokens();
}