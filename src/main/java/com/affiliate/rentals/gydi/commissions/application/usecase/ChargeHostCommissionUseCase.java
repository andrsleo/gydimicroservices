package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Charges host commission via Stripe Payment Intent.
 * <p>
 * Business Flow:
 * 1. Retrieve HostCommission (must be PENDING)
 * 2. Retrieve Host user and validate Stripe customer ID
 * 3. Retrieve Host's payment method (purpose=BOTH or HOST_COMMISSION)
 * 4. Charge via Stripe Payment Intent (off_session=true)
 * 5. Update commission status (CHARGED or FAILED)
 * </p>
 * <p>
 * Error Handling:
 * - No payment method → mark FAILED with reason
 * - Stripe error → mark FAILED with failure reason
 * - Success → mark CHARGED with transaction IDs
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ChargeHostCommissionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ChargeHostCommissionUseCase.class);

    private final HostCommissionRepositoryPort commissionRepository;
    private final ReferralCommissionRepositoryPort affiliateCommissionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    /**
     * Executes the host commission charge.
     *
     * @param commissionId ID of the commission to charge
     * @throws CommissionNotFoundException if commission not found
     * @throws IllegalStateException if commission already processed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long commissionId) {
        logger.info("Starting host commission charge for ID: {}", commissionId);

        // 1. Retrieve commission
        HostCommission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> CommissionNotFoundException.forId(commissionId));

        // 2. Validate commission is PENDING (idempotency check)
        if (commission.getStatus() != HostCommissionStatus.PENDING) {
            logger.warn("Commission {} is not PENDING (current status: {}), skipping charge",
                    commissionId, commission.getStatus());
            return;
        }

        // 3. Retrieve host's Connect account to get platform customer ID (cus_xxx)
        StripeConnectAccount connectAccount = connectAccountRepository
                .findByUserId(commission.getHostId()).orElse(null);

        if (connectAccount == null || connectAccount.getStripePlatformCustomerId() == null) {
            String reason = "Host does not have a Stripe Connect account with billing customer";
            logger.error("Cannot charge commission {}: {}", commissionId, reason);
            commission.markAsFailed(reason, 0, null, null);
            commissionRepository.save(commission);
            return;
        }

        // 4. Retrieve host's default payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findDefaultByUserId(commission.getHostId())
                .orElse(null);

        if (paymentMethod == null) {
            String reason = "Host does not have a payment method on file";
            logger.error("Cannot charge commission {}: {}", commissionId, reason);
            commission.markAsFailed(reason, 0, null, null);
            commissionRepository.save(commission);
            // TODO: Send email notification to host to add payment method
            return;
        }

        logger.info("Charging host commission {} via Stripe: amount={} {}, hostId={}, paymentMethod={}",
                commissionId,
                commission.getAmount().getCommissionAmount(),
                commission.getAmount().getCurrency(),
                commission.getHostId(),
                paymentMethod.cardLastFour());

        // 7. Update status to PROCESSING
        commission.markAsProcessing(null);
        commissionRepository.save(commission);

        try {
            // 8. Charge via Stripe Payment Intent
            // Convert BigDecimal amount to cents (Long) for Stripe
            long amountCents = commission.getAmount().getCommissionAmount()
                    .multiply(new java.math.BigDecimal("100"))
                    .longValue();

            PaymentGatewayPort.PaymentResult result = paymentGateway.chargeHostCommission(
                    connectAccount.getStripePlatformCustomerId(),
                    paymentMethod.gatewayToken(),
                    amountCents,
                    commission.getAmount().getCurrency(),
                    commission.getBookingId().toString(),
                    commissionId.toString()
            );

            // 9. Handle result
            if (result.success()) {
                commission.markAsCharged(
                        result.transactionId(),
                        result.chargeId(),
                        result.processedAt()
                );
                commissionRepository.save(commission);

                logger.info("Successfully charged host commission {}: transactionId={}, chargeId={}",
                        commissionId, result.transactionId(), result.chargeId());

                // Business rule: affiliate commission can only be approved after host charge succeeds
                approveAffiliateCommissionIfWaiting(commission.getBookingId());

                // TODO: Send email confirmation to host

            } else {
                // Use single-param markAsFailed: domain tracks attemptCount internally
                // (incremented by markAsProcessing), so exponential backoff is correct
                commission.markAsFailed(result.failureReason());
                commissionRepository.save(commission);

                logger.warn("Failed to charge host commission {}: reason={}, attemptCount={}, nextRetryAt={}",
                        commissionId, result.failureReason(),
                        commission.getAttemptCount(), commission.getNextRetryAt());

                // TODO: Send email notification to host about payment failure
            }

        } catch (Exception e) {
            logger.error("Exception while charging host commission {}", commissionId, e);

            commission.markAsFailed("System error: " + e.getMessage());
            commissionRepository.save(commission);
        }
    }

    /**
     * Approves the affiliate commission waiting for host charge confirmation.
     * <p>
     * Implements the business rule: affiliate commission is only eligible for payout
     * after the host commission has been successfully charged.
     * </p>
     *
     * @param bookingId the booking ID to find the associated affiliate commission
     */
    private void approveAffiliateCommissionIfWaiting(Long bookingId) {
        affiliateCommissionRepository.findByBookingId(bookingId)
            .ifPresent(affiliateCommission -> {
                if (affiliateCommission.getStatus() == ReferralCommissionStatus.WAITING_HOST_CHARGE) {
                    affiliateCommission.approveAfterHostCharged();
                    affiliateCommissionRepository.save(affiliateCommission);
                    logger.info("Affiliate commission ID: {} approved after host charge success for booking ID: {}",
                        affiliateCommission.getId(), bookingId);
                } else {
                    logger.debug("Affiliate commission for booking {} is in status {}, no approval needed",
                        bookingId, affiliateCommission.getStatus());
                }
            });
    }

}
