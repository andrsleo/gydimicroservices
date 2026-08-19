package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.BatchPayoutResultDto;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PayoutGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use Case: Processes batch payouts to affiliates via PayPal Payouts API.
 * <p>
 * Business Flow:
 * 1. Find eligible commissions (status=APPROVED, scheduled for paymentDate)
 * 2. Group by affiliate_id
 * 3. Calculate total per affiliate
 * 4. Filter affiliates with total >= $1.00 minimum (PayPal minimum)
 * 5. Verify affiliate has a PayPal email configured
 * 6. Send PayPal payout for each affiliate
 * 7. Update commissions to PAID status
 * </p>
 * <p>
 * Retry Logic:
 * - If payout fails, increment retry count on commissions
 * - After 3 failures, mark as WITHHELD (requires manual intervention)
 * </p>
 * <p>
 * Executed by scheduled job on 1st and 15th of month at 1 AM.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProcessReferralBatchPayoutUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessReferralBatchPayoutUseCase.class);

    private static final BigDecimal MIN_PAYOUT_AMOUNT = new BigDecimal("1.00"); // $1.00 minimum (PayPal minimum)
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ReferralCommissionRepositoryPort commissionRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PayoutGatewayPort payoutGateway;

    /**
     * Executes batch payout for the given payment date.
     *
     * @param paymentDate the payment date (1st or 15th)
     * @return BatchPayoutResultDto with success/failure counts
     */
    @Transactional
    public BatchPayoutResultDto execute(LocalDate paymentDate) {
        logger.info("========================================");
        logger.info("Starting batch payout process for date: {}", paymentDate);
        logger.info("========================================");

        // 1. Find eligible commissions
        List<ReferralCommission> eligibleCommissions = commissionRepository.findCommissionsForPayment(paymentDate);

        logger.info("Found {} eligible commissions for payout", eligibleCommissions.size());

        if (eligibleCommissions.isEmpty()) {
            logger.info("No commissions to process. Exiting.");
            return BatchPayoutResultDto.empty();
        }

        // 2. Group by affiliate_id
        Map<Long, List<ReferralCommission>> commissionsByAffiliate = eligibleCommissions.stream()
                .collect(Collectors.groupingBy(ReferralCommission::getAffiliateId));

        logger.info("Grouped commissions for {} unique affiliates", commissionsByAffiliate.size());

        int successfulPayouts = 0;
        int failedPayouts = 0;
        long totalAmountPaidCents = 0L;

        // 3. Process each affiliate
        for (Map.Entry<Long, List<ReferralCommission>> entry : commissionsByAffiliate.entrySet()) {
            Long affiliateId = entry.getKey();
            List<ReferralCommission> affiliateCommissions = entry.getValue();

            try {
                AffiliatePayoutResult result = processAffiliatePayouts(affiliateId, affiliateCommissions);

                if (result.success()) {
                    successfulPayouts++;
                    totalAmountPaidCents += result.amountPaidCents();
                } else {
                    failedPayouts++;
                }

            } catch (Exception e) {
                logger.error("Unexpected error processing payouts for affiliate {}: {}",
                        affiliateId, e.getMessage(), e);
                failedPayouts++;
            }
        }

        logger.info("========================================");
        logger.info("Batch payout process completed:");
        logger.info("  Successful payouts: {}", successfulPayouts);
        logger.info("  Failed payouts: {}", failedPayouts);
        logger.info("  Total paid: ${}", totalAmountPaidCents / 100.0);
        logger.info("========================================");

        return new BatchPayoutResultDto(
                successfulPayouts,
                failedPayouts,
                totalAmountPaidCents,
                "USD");
    }

    // ============================================================================
    // PRIVATE HELPERS
    // ============================================================================

    /**
     * Processes PayPal payout for a single affiliate.
     */
    private AffiliatePayoutResult processAffiliatePayouts(
            Long affiliateId,
            List<ReferralCommission> commissions) {

        // Calculate total amount
        BigDecimal totalAmount = commissions.stream()
                .map(c -> c.getAmount().getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCents = totalAmount.multiply(new BigDecimal("100")).longValue();

        logger.info("Processing payout for affiliate {}: {} commissions, total=${}",
                affiliateId, commissions.size(), totalAmount);

        // Check minimum payout amount
        if (totalAmount.compareTo(MIN_PAYOUT_AMOUNT) < 0) {
            logger.warn("Affiliate {} below minimum payout threshold (${}). Skipping.",
                    affiliateId, MIN_PAYOUT_AMOUNT);
            return AffiliatePayoutResult.skipped("Below minimum threshold");
        }

        // Get affiliate's payout account (for PayPal email)
        StripeConnectAccount account = connectAccountRepository.findByUserId(affiliateId).orElse(null);

        if (account == null || !account.hasPayPalEmail()) {
            logger.warn("Affiliate {} has no PayPal email configured. Skipping payout — will retry when email is set.",
                    affiliateId);
            // Do NOT withhold — leave as APPROVED so scheduler retries once email is configured
            return AffiliatePayoutResult.skipped("No PayPal email configured");
        }

        // Get currency (assume all commissions same currency)
        String currency = commissions.stream()
                .findFirst()
                .map(c -> c.getAmount().getCurrency())
                .orElse("USD");

        // Build commission IDs for the note
        String commissionIds = commissions.stream()
                .map(c -> c.getId().toString())
                .collect(Collectors.joining(","));

        String note = String.format("Comisiones GYDI #%s - $%s %s", commissionIds, totalAmount, currency);

        // Send PayPal payout
        try {
            PayoutGatewayPort.PayoutResult payoutResult = payoutGateway.sendPayout(
                    account.getPaypalEmail(),
                    totalAmount,
                    currency,
                    "batch-" + affiliateId + "-" + LocalDate.now(),
                    note
            );

            if (payoutResult.success()) {
                // Mark all commissions as PAID with PayPal batch IDs
                commissions.forEach(commission -> {
                    commission.markAsPaid(payoutResult.payoutBatchId(), payoutResult.payoutItemId());
                    commissionRepository.save(commission);
                });

                logger.info("PayPal payout successful for affiliate {}: batchId={}, amount=${}",
                        affiliateId, payoutResult.payoutBatchId(), totalAmount);

                return AffiliatePayoutResult.success(totalCents);

            } else {
                logger.warn("PayPal payout failed for affiliate {}: reason={}",
                        affiliateId, payoutResult.failureReason());

                commissions.forEach(commission -> {
                    commission.recordPaymentFailure(payoutResult.failureReason());

                    if (commission.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
                        logger.error("Commission {} reached max retries ({}), marked as WITHHELD",
                                commission.getId(), MAX_RETRY_ATTEMPTS);
                    }

                    commissionRepository.save(commission);
                });

                return AffiliatePayoutResult.failed(payoutResult.failureReason());
            }

        } catch (Exception e) {
            logger.error("Exception during PayPal payout for affiliate {}: {}",
                    affiliateId, e.getMessage(), e);

            markCommissionsAsWithheld(commissions, "System error: " + e.getMessage());
            return AffiliatePayoutResult.failed("System error");
        }
    }

    /**
     * Marks commissions as WITHHELD (terminal state, requires manual intervention).
     */
    private void markCommissionsAsWithheld(List<ReferralCommission> commissions, String reason) {
        commissions.forEach(commission -> {
            commission.withhold(reason);
            commissionRepository.save(commission);
            logger.warn("Commission {} marked as WITHHELD: {}", commission.getId(), reason);
        });
    }

    /**
     * Internal result record for per-affiliate processing.
     */
    private record AffiliatePayoutResult(boolean success, long amountPaidCents, String failureReason) {
        static AffiliatePayoutResult success(long amountPaidCents) {
            return new AffiliatePayoutResult(true, amountPaidCents, null);
        }

        static AffiliatePayoutResult failed(String reason) {
            return new AffiliatePayoutResult(false, 0L, reason);
        }

        static AffiliatePayoutResult skipped(String reason) {
            return new AffiliatePayoutResult(false, 0L, reason);
        }
    }
}
