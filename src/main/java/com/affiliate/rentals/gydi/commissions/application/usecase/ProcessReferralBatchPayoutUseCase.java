package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.BatchPayoutResultDto;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use Case: Processes batch payouts to referrers (affiliates).
 * <p>
 * Business Flow:
 * 1. Find eligible commissions (status=APPROVED, scheduled for paymentDate)
 * 2. Group by affiliate_id
 * 3. Calculate total per affiliate
 * 4. Filter affiliates with total >= $50 minimum
 * 5. Verify affiliate has Connect Account with payouts enabled
 * 6. Create Stripe Transfer for each affiliate
 * 7. Update commissions to PAID status
 * </p>
 * <p>
 * Retry Logic:
 * - If transfer fails, increment retry count
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

    private static final long MIN_PAYOUT_AMOUNT_CENTS = 5000L; // $50.00 minimum
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ReferralCommissionRepositoryPort commissionRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PaymentGatewayPort paymentGateway;

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
        long totalAmountPaid = 0L;

        // 3. Process each affiliate
        for (Map.Entry<Long, List<ReferralCommission>> entry : commissionsByAffiliate.entrySet()) {
            Long affiliateId = entry.getKey();
            List<ReferralCommission> affiliateCommissions = entry.getValue();

            try {
                PayoutResult result = processAffiliatePayouts(affiliateId, affiliateCommissions, paymentDate);

                if (result.success()) {
                    successfulPayouts++;
                    totalAmountPaid += result.amountPaid();
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
        logger.info("  Total paid: ${}", totalAmountPaid / 100.0);
        logger.info("========================================");

        return new BatchPayoutResultDto(
                successfulPayouts,
                failedPayouts,
                totalAmountPaid,
                "USD");
    }

    /**
     * Processes payouts for a single affiliate.
     *
     * @param affiliateId        the affiliate user ID
     * @param commissions        list of commissions for this affiliate
     * @param paymentDate        the payment date
     * @return PayoutResult with success/failure info
     */
    private PayoutResult processAffiliatePayouts(
            Long affiliateId,
            List<ReferralCommission> commissions,
            LocalDate paymentDate) {
        // Calculate total amount (convert BigDecimal to cents)
        long totalCents = commissions.stream()
                .mapToLong(c -> c.getAmount().getCommissionAmount()
                        .multiply(new java.math.BigDecimal("100"))
                        .longValue())
                .sum();

        logger.info("Processing payout for affiliate {}: {} commissions, total=${} ({}c)",
                affiliateId, commissions.size(), totalCents / 100.0, totalCents);

        // Check minimum payout amount
        if (totalCents < MIN_PAYOUT_AMOUNT_CENTS) {
            logger.warn("Affiliate {} below minimum payout threshold: ${}. Skipping.",
                    affiliateId, totalCents / 100.0);
            return PayoutResult.skipped("Below minimum threshold");
        }

        // Get Connect Account
        StripeConnectAccount connectAccount = connectAccountRepository.findByUserId(affiliateId)
                .orElse(null);

        if (connectAccount == null) {
            logger.error("Affiliate {} has no Connect Account. Cannot process payout.", affiliateId);
            markCommissionsAsWithheld(commissions, "No Connect Account");
            return PayoutResult.failed("No Connect Account");
        }

        if (!connectAccount.canReceivePayouts()) {
            logger.error("Affiliate {} Connect Account cannot receive payouts (onboarding incomplete or disabled).",
                    affiliateId);
            markCommissionsAsWithheld(commissions, "Connect Account not ready for payouts");
            return PayoutResult.failed("Connect Account not ready");
        }

        // Get currency (assume all commissions same currency)
        String currency = commissions.stream()
                .findFirst()
                .map(c -> c.getAmount().getCurrency())
                .orElse("USD");

        // Create comma-separated list of commission IDs for metadata
        String commissionIds = commissions.stream()
                .map(c -> c.getId().toString())
                .collect(Collectors.joining(","));

        // Execute Stripe Transfer
        try {
            PaymentGatewayPort.PaymentResult transferResult = paymentGateway.transferToAffiliate(
                    connectAccount.getStripeAccountId(),
                    totalCents,
                    currency,
                    commissionIds);

            if (transferResult.success()) {
                // Mark commissions as PAID
                commissions.forEach(commission -> {
                    commission.markAsPaid(transferResult.transactionId(), null);
                    commissionRepository.save(commission);
                });

                logger.info("Transfer successful for affiliate {}: transferId={}, amount=${}",
                        affiliateId, transferResult.transactionId(), totalCents / 100.0);

                return PayoutResult.success(totalCents);

            } else {
                // Transfer failed
                logger.warn("Transfer failed for affiliate {}: reason={}",
                        affiliateId, transferResult.failureReason());

                // Record payment failures (auto-increments attemptCount)
                commissions.forEach(commission -> {
                    commission.recordPaymentFailure(transferResult.failureReason());

                    if (commission.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
                        logger.error("Commission {} reached max retries ({}), marked as WITHHELD",
                                commission.getId(), MAX_RETRY_ATTEMPTS);
                    }

                    commissionRepository.save(commission);
                });

                return PayoutResult.failed(transferResult.failureReason());
            }

        } catch (Exception e) {
            logger.error("Exception during Stripe Transfer for affiliate {}: {}",
                    affiliateId, e.getMessage(), e);

            markCommissionsAsWithheld(commissions, "System error: " + e.getMessage());

            return PayoutResult.failed("System error");
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
     * Internal result record for processing.
     */
    private record PayoutResult(boolean success, long amountPaid, String failureReason) {
        static PayoutResult success(long amountPaid) {
            return new PayoutResult(true, amountPaid, null);
        }

        static PayoutResult failed(String reason) {
            return new PayoutResult(false, 0L, reason);
        }

        static PayoutResult skipped(String reason) {
            return new PayoutResult(false, 0L, reason);
        }
    }
}
