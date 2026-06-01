package com.affiliate.rentals.gydi.collaborations.infrastructure.scheduler;

import com.affiliate.rentals.gydi.collaborations.application.usecase.ExpirePitchesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to expire pitches that have not been responded to within 7 days.
 *
 * <p>Runs daily at 02:00 UTC. Follows the same pattern as CommissionPaymentScheduler.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PitchExpirationScheduler {

    private final ExpirePitchesUseCase expirePitchesUseCase;

    /**
     * Daily expiration sweep at 02:00 UTC.
     * Finds all pitches in PENDING or COUNTERED status whose {@code expiresAt} is in the past
     * and transitions them to EXPIRED.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void expireOverduePitches() {
        log.info("[PitchExpirationScheduler] Running daily pitch expiration sweep");
        try {
            expirePitchesUseCase.execute();
        } catch (Exception e) {
            log.error("[PitchExpirationScheduler] Expiration sweep failed: {}", e.getMessage(), e);
        }
    }
}
