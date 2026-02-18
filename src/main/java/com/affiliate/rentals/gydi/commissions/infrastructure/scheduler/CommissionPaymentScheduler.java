package com.affiliate.rentals.gydi.commissions.infrastructure.scheduler;

import com.affiliate.rentals.gydi.commissions.application.usecase.ChargeHostCommissionUseCase;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled job for retrying failed host commission charges.
 * <p>
 * Runs daily at 2 AM to retry failed commission charges with exponential backoff.
 * </p>
 * <p>
 * Retry Schedule:
 * - Attempt 1: Immediate (via webhook)
 * - Attempt 2: +24 hours (this scheduler)
 * - Attempt 3: +72 hours (this scheduler)
 * - After 3 failures: Property SUSPENDED, commission marked WITHHELD
 * </p>
 * <p>
 * Monitoring:
 * - Logs success/failure counts
 * - Tracks properties suspended due to payment failures
 * - TODO: Send alerts to monitoring system (Slack, PagerDuty)
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CommissionPaymentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CommissionPaymentScheduler.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final HostCommissionRepositoryPort commissionRepository;
    private final ReferralCommissionRepositoryPort affiliateCommissionRepository;
    private final ChargeHostCommissionUseCase chargeHostCommissionUseCase;

    /**
     * Retries failed host commission charges.
     * <p>
     * Runs daily at 2 AM server time.
     * </p>
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void retryFailedHostCharges() {
        logger.info("========================================");
        logger.info("Starting commission payment retry job");
        logger.info("========================================");

        LocalDateTime now = LocalDateTime.now();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger maxRetriesCount = new AtomicInteger(0);

        try {
            // Find all FAILED commissions ready for retry
            List<HostCommission> failedCommissions = commissionRepository.findCommissionsForRetry(now);

            logger.info("Found {} failed commissions eligible for retry", failedCommissions.size());

            if (failedCommissions.isEmpty()) {
                logger.info("No failed commissions to retry");
                return;
            }

            for (HostCommission commission : failedCommissions) {
                processFailedCommission(commission, successCount, failedCount, maxRetriesCount);
            }

            // Log summary
            logger.info("========================================");
            logger.info("Commission retry job completed:");
            logger.info("  ✓ Successful retries: {}", successCount.get());
            logger.info("  ✗ Failed retries: {}", failedCount.get());
            logger.info("  ⚠ Max retries reached: {}", maxRetriesCount.get());
            logger.info("========================================");

            // TODO: Send summary to monitoring system

        } catch (Exception e) {
            logger.error("========================================");
            logger.error("Fatal error in commission retry job: {}", e.getMessage(), e);
            logger.error("========================================");

            // TODO: Send alert to ops team
        }
    }

    /**
     * Processes a single failed commission.
     *
     * @param commission the failed commission
     * @param successCount counter for successful retries
     * @param failedCount counter for failed retries
     * @param maxRetriesCount counter for commissions that reached max retries
     */
    private void processFailedCommission(
            HostCommission commission,
            AtomicInteger successCount,
            AtomicInteger failedCount,
            AtomicInteger maxRetriesCount
    ) {
        Long commissionId = commission.getId();
        int attemptCount = commission.getAttemptCount();

        logger.info("Processing failed commission: ID={}, booking={}, attempts={}/{}",
                commissionId,
                commission.getBookingId(),
                attemptCount,
                MAX_RETRY_ATTEMPTS);

        // Check if max retries reached
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            handleMaxRetriesReached(commission);
            maxRetriesCount.incrementAndGet();
            return;
        }

        // Check if it's time to retry
        if (commission.getNextRetryAt() == null || LocalDateTime.now().isBefore(commission.getNextRetryAt())) {
            logger.debug("Commission {} not yet ready for retry (next retry: {})",
                    commissionId, commission.getNextRetryAt());
            return;
        }

        // Attempt retry
        try {
            logger.info("Retrying charge for commission {} (attempt {} of {})",
                    commissionId, attemptCount + 1, MAX_RETRY_ATTEMPTS);

            chargeHostCommissionUseCase.execute(commissionId);

            // If we reach here, charge was attempted
            // Result will be updated by ChargeHostCommissionUseCase
            // Check final status
            HostCommission updated = commissionRepository.findById(commissionId)
                    .orElse(commission);

            if (updated.getStatus() == HostCommissionStatus.CHARGED) {
                logger.info("✓ Commission {} charged successfully on retry", commissionId);
                successCount.incrementAndGet();
                // TODO: Send success notification to host
            } else if (updated.getStatus() == HostCommissionStatus.FAILED) {
                logger.warn("✗ Commission {} failed again on retry", commissionId);
                failedCount.incrementAndGet();
                // TODO: Send failure notification to host
            }

        } catch (Exception e) {
            logger.error("Exception while retrying commission {}: {}", commissionId, e.getMessage(), e);
            failedCount.incrementAndGet();
        }
    }

    /**
     * Handles commission that reached max retry attempts.
     * <p>
     * Actions:
     * 1. Mark host commission as WITHHELD (terminal state, no more retries)
     * 2. Cascade: withhold the affiliate commission (platform never collected from host)
     * 3. Suspend property (no new bookings accepted)
     * 4. Send urgent notification to host
     * </p>
     *
     * @param commission the commission that reached max retries
     */
    private void handleMaxRetriesReached(HostCommission commission) {
        Long commissionId = commission.getId();
        Long bookingId = commission.getBookingId();

        logger.error("========================================");
        logger.error("Max retries reached for commission: ID={}, booking={}",
                commissionId, bookingId);
        logger.error("Commission amount: {} {}",
                commission.getAmount().getCommissionAmount(),
                commission.getAmount().getCurrency());
        logger.error("Last failure reason: {}", commission.getFailureReason());
        logger.error("========================================");

        // 1. Mark host commission as WITHHELD (terminal state, requires manual intervention)
        String withholdReason = "Max retry attempts (" + MAX_RETRY_ATTEMPTS + ") exceeded";
        commission.withhold(withholdReason);
        commissionRepository.save(commission);
        logger.warn("Host commission {} marked as WITHHELD after max retries for booking {}",
                commissionId, bookingId);

        // 2. Business rule cascade: withhold affiliate commission since host never paid
        // The platform cannot pay the affiliate if it never collected from the host.
        String affiliateWithholdReason = "Host commission for booking " + bookingId +
            " was not collected after " + MAX_RETRY_ATTEMPTS +
            " attempts. Affiliate payment withheld.";
        affiliateCommissionRepository.findByBookingId(bookingId)
            .ifPresent(affiliateCommission -> {
                if (affiliateCommission.getStatus() == ReferralCommissionStatus.WAITING_HOST_CHARGE) {
                    affiliateCommission.withholdDueToHostChargeFailure(affiliateWithholdReason);
                    affiliateCommissionRepository.save(affiliateCommission);
                    logger.warn("Affiliate commission ID: {} withheld because host commission {} reached max retries for booking {}",
                        affiliateCommission.getId(), commissionId, bookingId);
                }
            });

        // TODO: Suspend property (block new bookings)
        //    propertyService.suspendProperty(commission.getPropertyId(), "Payment failure - max retries");

        // TODO: Send urgent notification to host
        //    emailService.sendMaxRetriesAlert(commission.getHostId(), commission);
        //    notificationService.sendInAppAlert(commission.getHostId(), "URGENT: Payment failed - Property suspended");

        // TODO: Alert ops team
        //    alertingService.sendOpsAlert("Commission max retries", commission);

        logger.warn("Commission {} requires manual intervention. Host {} should be contacted to update payment method.",
                commissionId, commission.getHostId());
    }

    /**
     * Health check method for monitoring.
     * <p>
     * Can be called by external monitoring systems to verify scheduler is active.
     * </p>
     *
     * @return health status
     */
    public SchedulerHealthStatus getHealthStatus() {
        try {
            // Count failed commissions awaiting retry
            LocalDateTime now = LocalDateTime.now();
            List<HostCommission> pending = commissionRepository.findCommissionsForRetry(now);

            return new SchedulerHealthStatus(
                    true,
                    "Scheduler is healthy",
                    pending.size(),
                    now
            );

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return new SchedulerHealthStatus(
                    false,
                    "Error: " + e.getMessage(),
                    0,
                    LocalDateTime.now()
            );
        }
    }

    /**
     * Health status record for monitoring.
     */
    public record SchedulerHealthStatus(
            boolean healthy,
            String message,
            int pendingRetries,
            LocalDateTime checkedAt
    ) {}
}
