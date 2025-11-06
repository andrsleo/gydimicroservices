package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.PasswordResetToken;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for password reset token repository operations.
 *
 * <p>This port defines the contract for persistence operations related to password reset tokens.
 * It follows the hexagonal architecture pattern, allowing the domain layer to remain independent
 * of infrastructure concerns. Implementations of this interface will be provided by adapters
 * in the infrastructure layer.</p>
 *
 * <p>This interface provides methods for:</p>
 * <ul>
 *   <li>Saving and updating password reset tokens</li>
 *   <li>Finding tokens by various criteria</li>
 *   <li>Deleting expired or used tokens</li>
 *   <li>Checking token validity</li>
 * </ul>
 *
 * <p>Example usage in use cases:</p>
 * <pre>{@code
 * public class RequestPasswordResetUseCase {
 *     private final PasswordResetTokenRepositoryPort tokenRepository;
 *
 *     public void execute(Long userId) {
 *         PasswordResetToken token = PasswordResetToken.builder()
 *             .userId(userId)
 *             .expirationMinutes(60)
 *             .build();
 *
 *         tokenRepository.save(token);
 *     }
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 * @see PasswordResetToken
 */
public interface PasswordResetTokenRepositoryPort {

    /**
     * Saves a new password reset token or updates an existing one.
     *
     * @param token the token to save
     * @return the saved token with generated ID (if new)
     * @throws IllegalArgumentException if token is null
     */
    PasswordResetToken save(PasswordResetToken token);

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the token if found, empty otherwise
     * @throws IllegalArgumentException if token is null or empty
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Finds the most recent valid (non-used, non-expired) token for a user.
     * This is useful for checking if a user already has a pending reset request.
     *
     * @param userId the user ID
     * @return an Optional containing the latest valid token if found, empty otherwise
     * @throws IllegalArgumentException if userId is null
     */
    Optional<PasswordResetToken> findLatestValidTokenByUserId(Long userId);

    /**
     * Finds all tokens for a specific user, ordered by creation date descending.
     * This can be used for auditing or displaying reset history.
     *
     * @param userId the user ID
     * @return list of tokens for the user, empty list if none found
     * @throws IllegalArgumentException if userId is null
     */
    List<PasswordResetToken> findAllByUserId(Long userId);

    /**
     * Invalidates all existing tokens for a user.
     * This is typically called after a successful password reset or when the user
     * requests a new token while having pending ones.
     *
     * @param userId the user ID
     * @return the number of tokens invalidated
     * @throws IllegalArgumentException if userId is null
     */
    int invalidateAllTokensForUser(Long userId);

    /**
     * Deletes all expired tokens from the database.
     * This method is typically called by a scheduled job for cleanup.
     *
     * @return the number of tokens deleted
     */
    int deleteExpiredTokens();

    /**
     * Deletes all used tokens that were marked as used before the specified days.
     * This helps maintain database size by removing old, already-used tokens.
     *
     * @param daysOld the number of days to look back
     * @return the number of tokens deleted
     * @throws IllegalArgumentException if daysOld is negative
     */
    int deleteUsedTokensOlderThan(int daysOld);

    /**
     * Checks if a user has any valid (non-expired, non-used) tokens.
     *
     * @param userId the user ID
     * @return true if the user has at least one valid token, false otherwise
     * @throws IllegalArgumentException if userId is null
     */
    boolean hasValidToken(Long userId);

    /**
     * Counts the number of password reset requests for a user within a time window.
     * This is used for rate limiting to prevent abuse.
     *
     * @param userId the user ID
     * @param hoursBack the number of hours to look back
     * @return the count of requests within the time window
     * @throws IllegalArgumentException if userId is null or hoursBack is negative
     */
    int countRequestsInLastHours(Long userId, int hoursBack);
}