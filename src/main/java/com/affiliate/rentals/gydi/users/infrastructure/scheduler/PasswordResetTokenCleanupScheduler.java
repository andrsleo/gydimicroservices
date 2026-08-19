package com.affiliate.rentals.gydi.users.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.users.domain.ports.PasswordResetTokenRepositoryPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled task for cleaning up expired and old password reset tokens.
 *
 * <p>This scheduler runs daily at 2:00 AM to perform maintenance on the password reset tokens table:</p>
 * <ul>
 *   <li>Removes all expired tokens (past expiration date)</li>
 *   <li>Removes used tokens older than 30 days (for compliance and database optimization)</li>
 * </ul>
 *
 * <p>Benefits:</p>
 * <ul>
 *   <li>Prevents database bloat</li>
 *   <li>Improves query performance</li>
 *   <li>Maintains data retention policies</li>
 *   <li>Reduces storage costs</li>
 * </ul>
 *
 * <p>Scheduling configuration:</p>
 * <ul>
 *   <li>Cron: 0 0 2 * * * (2:00 AM every day)</li>
 *   <li>Timezone: System default</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Component
public class PasswordResetTokenCleanupScheduler {
    
    private static final int USED_TOKEN_RETENTION_DAYS = 30;

    private final PasswordResetTokenRepositoryPort tokenRepository;

    public PasswordResetTokenCleanupScheduler(PasswordResetTokenRepositoryPort tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Scheduled cleanup task that runs daily at 2:00 AM.
     * Removes expired tokens and old used tokens.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of password reset tokens");

        try {
            // Delete expired tokens
            int expiredCount = tokenRepository.deleteExpiredTokens();
            log.info("Deleted {} expired password reset token(s)", expiredCount);

            // Delete used tokens older than retention period
            int oldUsedCount = tokenRepository.deleteUsedTokensOlderThan(USED_TOKEN_RETENTION_DAYS);
            log.info("Deleted {} used password reset token(s) older than {} days",
                    oldUsedCount, USED_TOKEN_RETENTION_DAYS);

            int totalDeleted = expiredCount + oldUsedCount;
            log.info("Password reset token cleanup completed successfully. Total deleted: {}", totalDeleted);

        } catch (Exception e) {
            log.error("Error during password reset token cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup method that can be called on-demand.
     * Useful for testing or administrative operations.
     *
     * @return the total number of tokens deleted
     */
    public int performManualCleanup() {
        log.info("Performing manual cleanup of password reset tokens");

        try {
            int expiredCount = tokenRepository.deleteExpiredTokens();
            int oldUsedCount = tokenRepository.deleteUsedTokensOlderThan(USED_TOKEN_RETENTION_DAYS);
            int totalDeleted = expiredCount + oldUsedCount;

            log.info("Manual cleanup completed. Deleted {} expired and {} old used tokens",
                    expiredCount, oldUsedCount);

            return totalDeleted;

        } catch (Exception e) {
            log.error("Error during manual cleanup: {}", e.getMessage(), e);
            throw e;
        }
    }
}
