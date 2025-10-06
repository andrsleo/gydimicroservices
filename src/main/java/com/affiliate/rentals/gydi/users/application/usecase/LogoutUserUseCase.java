package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.shared.security.TokenBlacklistService;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for logging out a user.
 *
 * <p>This use case handles user logout by blacklisting the current access token
 * and optionally revoking all refresh tokens for the user.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class LogoutUserUseCase {

    private static final Logger logger = LoggerFactory.getLogger(LogoutUserUseCase.class);

    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUserUseCase(
            TokenBlacklistService tokenBlacklistService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Executes the logout operation.
     *
     * <p>Supports two logout strategies following Java 21 best practices:</p>
     * <ul>
     *   <li>Simple: Blacklists only the current access token</li>
     *   <li>Complete: Blacklists token + revokes all refresh tokens</li>
     * </ul>
     *
     * @param accessToken the current access token to blacklist
     * @param userId the ID of the user logging out (optional)
     * @param revokeAllDevices whether to revoke all refresh tokens (logout all devices)
     */
    @Transactional
    public void execute(String accessToken, Long userId, boolean revokeAllDevices) {
        logger.debug("Executing logout for userId: {}, revokeAllDevices: {}", userId, revokeAllDevices);

        // Blacklist the current access token
        tokenBlacklistService.blacklistToken(accessToken);
        logger.info("Access token blacklisted successfully");

        // Optionally revoke all refresh tokens for the user
        if (revokeAllDevices && userId != null) {
            int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
            logger.info("Revoked {} refresh token(s) for userId: {}", revokedCount, userId);

            if (revokedCount == 0) {
                logger.warn("No refresh tokens found to revoke for userId: {}", userId);
            }
        }
    }

    /**
     * Executes the logout operation for current device only.
     *
     * @param accessToken the current access token to blacklist
     */
    @Transactional
    public void execute(String accessToken) {
        // Just blacklist the token without revoking refresh tokens
        tokenBlacklistService.blacklistToken(accessToken);
    }
}