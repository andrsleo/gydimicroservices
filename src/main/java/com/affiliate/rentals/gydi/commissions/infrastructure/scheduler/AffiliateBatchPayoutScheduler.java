package com.affiliate.rentals.gydi.commissions.infrastructure.scheduler;

import com.affiliate.rentals.gydi.commissions.application.dto.BatchPayoutResultDto;
import com.affiliate.rentals.gydi.commissions.application.usecase.ProcessReferralBatchPayoutUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduled job for processing batch payouts to affiliates.
 * <p>
 * Runs on the 1st and 15th of each month at 1 AM server time.
 * </p>
 * <p>
 * Payout Schedule:
 * - Day 1: Commissions from previous month (16th-31st/30th)
 * - Day 15: Commissions from current month (1st-15th)
 * </p>
 * <p>
 * Requirements:
 * - Commission status = APPROVED
 * - Dispute period ended (7 days after booking finished)
 * - Affiliate total >= $50 minimum
 * - Affiliate has Connect Account with payouts enabled
 * </p>
 * <p>
 * Monitoring:
 * - Logs success/failure counts
 * - Tracks total amount paid
 * - TODO: Send summary email to ops team
 * - TODO: Send payout notifications to affiliates
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AffiliateBatchPayoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AffiliateBatchPayoutScheduler.class);

    private final ProcessReferralBatchPayoutUseCase batchPayoutUseCase;

    /**
     * Processes batch payouts on 1st and 15th of month.
     * <p>
     * Cron expression: "0 0 1 1,15 * ?"
     * - Second: 0
     * - Minute: 0
     * - Hour: 1 (1 AM)
     * - Day of month: 1,15 (1st and 15th)
     * - Month: * (every month)
     * - Day of week: ? (ignored)
     * </p>
     */
    @Scheduled(cron = "0 0 1 1,15 * ?") // 1 AM on 1st and 15th
    public void processBatchPayouts() {
        LocalDate today = LocalDate.now();
        LocalDateTime startTime = LocalDateTime.now();

        logger.info("========================================");
        logger.info("Starting batch payout job for: {}", today);
        logger.info("Job started at: {}", startTime);
        logger.info("========================================");

        try {
            // Execute batch payout
            BatchPayoutResultDto result = batchPayoutUseCase.execute(today);

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            // Log summary
            logger.info("========================================");
            logger.info("Batch payout job completed successfully");
            logger.info("  Date: {}", today);
            logger.info("  ✓ Successful payouts: {}", result.successfulPayouts());
            logger.info("  ✗ Failed payouts: {}", result.failedPayouts());
            logger.info("  💰 Total paid: ${} {}", result.totalAmountPaid() / 100.0, result.currency());
            logger.info("  ⏱ Duration: {}s", durationSeconds);
            logger.info("========================================");

            // TODO: Send summary email to ops team
            // opsNotificationService.sendBatchPayoutSummary(result);

            // TODO: Send payout notifications to affiliates
            // affiliateNotificationService.sendPayoutNotifications(result);

        } catch (Exception e) {
            logger.error("========================================");
            logger.error("CRITICAL: Batch payout job failed for date: {}", today);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("========================================");

            // TODO: Send critical alert to ops team
            // alertingService.sendCriticalAlert("Batch Payout Job Failed", e);
        }
    }

    /**
     * Manual trigger endpoint for testing/recovery.
     * <p>
     * Can be called by ADMIN to manually trigger batch payout for a specific date.
     * Useful for:
     * - Testing in non-production environments
     * - Recovery after job failures
     * - Processing missed payouts
     * </p>
     *
     * @param payoutDate the date to process payouts for
     * @return BatchPayoutResultDto with results
     */
    public BatchPayoutResultDto triggerManualPayout(LocalDate payoutDate) {
        logger.warn("========================================");
        logger.warn("MANUAL TRIGGER: Batch payout for date: {}", payoutDate);
        logger.warn("========================================");

        try {
            BatchPayoutResultDto result = batchPayoutUseCase.execute(payoutDate);

            logger.info("Manual batch payout completed: {} successful, {} failed",
                result.successfulPayouts(), result.failedPayouts());

            return result;

        } catch (Exception e) {
            logger.error("Manual batch payout failed for date {}: {}", payoutDate, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Health check method for monitoring.
     * <p>
     * Returns status of next scheduled payout.
     * </p>
     *
     * @return SchedulerHealthStatus
     */
    public SchedulerHealthStatus getHealthStatus() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate nextPayoutDate = calculateNextPayoutDate(today);

            return new SchedulerHealthStatus(
                true,
                "Scheduler is healthy",
                nextPayoutDate,
                LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return new SchedulerHealthStatus(
                false,
                "Error: " + e.getMessage(),
                null,
                LocalDateTime.now()
            );
        }
    }

    /**
     * Calculates next payout date (1st or 15th).
     */
    private LocalDate calculateNextPayoutDate(LocalDate today) {
        int dayOfMonth = today.getDayOfMonth();

        if (dayOfMonth < 1) {
            // Before 1st → next payout is 1st of current month
            return LocalDate.of(today.getYear(), today.getMonth(), 1);
        } else if (dayOfMonth < 15) {
            // Between 1st and 14th → next payout is 15th of current month
            return LocalDate.of(today.getYear(), today.getMonth(), 15);
        } else {
            // After 15th → next payout is 1st of next month
            return today.plusMonths(1).withDayOfMonth(1);
        }
    }

    /**
     * Health status record for monitoring.
     */
    public record SchedulerHealthStatus(
        boolean healthy,
        String message,
        LocalDate nextPayoutDate,
        LocalDateTime checkedAt
    ) {}
}
