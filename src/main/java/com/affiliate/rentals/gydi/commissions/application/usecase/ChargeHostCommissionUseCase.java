package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
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
    private final UserRepositoryPort userRepository;
    private final BookingRepositoryPort bookingRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

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

        // 3. Retrieve host's Connect account to get acct_xxx AND cus_xxx
        StripeConnectAccount connectAccount = connectAccountRepository
                .findByUserId(commission.getHostId()).orElse(null);

        if (connectAccount == null) {
            String reason = "Host does not have a Stripe Connect account";
            logger.error("Cannot charge commission {}: {}", commissionId, reason);
            commission.markAsFailed(reason, 0, null, null);
            commissionRepository.save(commission);
            return;
        }

        if (connectAccount.getStripePlatformCustomerId() == null) {
            String reason = "Host Stripe Connect account has no billing customer (cus_xxx). Host must add a payment method.";
            logger.error("Cannot charge commission {}: {}", commissionId, reason);
            commission.markAsFailed(reason, 0, null, null);
            commissionRepository.save(commission);
            return;
        }

        // The host must have a cus_xxx to be charged on the platform.
        // We do NOT require card_payments on the Connect account because we charge
        // the host directly via the platform account (not on_behalf_of the Connect account).
        // The Connect account (acct_xxx) is only needed to PAY the host as an affiliate.

        // 4. Retrieve host's default payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findDefaultByUserId(commission.getHostId())
                .orElse(null);

        if (paymentMethod == null) {
            String reason = "Host does not have a payment method on file";
            logger.error("Cannot charge commission {}: {}", commissionId, reason);
            commission.markAsFailed(reason, 0, null, null);
            commissionRepository.save(commission);
            return;
        }

        logger.info("Charging host commission {}: commissionAmount={} {}, hostId={}, customer={}, paymentMethod={}",
                commissionId,
                commission.getAmount().getCommissionAmount(),
                commission.getAmount().getCurrency(),
                commission.getHostId(),
                connectAccount.getStripePlatformCustomerId(),
                paymentMethod.cardLastFour());

        // 7. Update status to PROCESSING
        commission.markAsProcessing(null);
        commissionRepository.save(commission);

        try {
            // 8. Charge commission to host directly on the platform account.
            // GYDI does NOT process booking payments through Stripe — the host pays the
            // platform a commission fee (15%/20%/25%) separately.
            // We charge ONLY the commission amount, NOT the full booking amount.
            // on_behalf_of / chargeHostViaConnect is NOT used here because it requires
            // card_payments capability; Express accounts only have transfers capability.
            long commissionAmountCents = commission.getAmount().getCommissionAmount()
                    .multiply(new java.math.BigDecimal("100"))
                    .longValue();

            PaymentGatewayPort.PaymentResult result = paymentGateway.chargeHostCommission(
                    connectAccount.getStripePlatformCustomerId(),
                    paymentMethod.gatewayToken(),
                    commissionAmountCents,
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

                // Fire-and-forget: send commission charged email to host
                sendHostCommissionChargedEmail(commission);

            } else {
                // Use single-param markAsFailed: domain tracks attemptCount internally
                // (incremented by markAsProcessing), so exponential backoff is correct
                commission.markAsFailed(result.failureReason());
                commissionRepository.save(commission);

                logger.warn("Failed to charge host commission {}: reason={}, attemptCount={}, nextRetryAt={}",
                        commissionId, result.failureReason(),
                        commission.getAttemptCount(), commission.getNextRetryAt());
            }

        } catch (Exception e) {
            logger.error("Exception while charging host commission {}", commissionId, e);

            commission.markAsFailed("System error: " + e.getMessage());
            commissionRepository.save(commission);
        }
    }

    /**
     * Sends a host-commission-charged email. Fire-and-forget: never throws.
     */
    private void sendHostCommissionChargedEmail(HostCommission commission) {
        try {
            userRepository.findById(commission.getHostId()).ifPresent(host -> {
                // Resolve property title via booking → property chain
                String propertyTitle = bookingRepository.findById(commission.getBookingId())
                        .flatMap(booking -> propertyRepository.findById(PropertyId.of(booking.getPropertyId())))
                        .map(property -> property.getTitle())
                        .orElse("tu propiedad");

                EmailMessage email = emailTemplateService.buildHostCommissionChargedEmail(
                        host.email().address(),
                        new EmailTemplateService.HostCommissionChargedEmailData(
                                host.name(),
                                propertyTitle,
                                commission.getAmount().getCommissionAmount(),
                                commission.getAmount().getBookingAmount(),
                                commission.getAmount().getCurrency()
                        )
                );
                emailService.sendEmail(email);
                logger.info("Host commission charged email sent to host {} for commission {}",
                        commission.getHostId(), commission.getId());
            });
        } catch (Exception e) {
            logger.error("Failed to send host commission charged email for commission {}: {}",
                    commission.getId(), e.getMessage());
        }
    }

    /**
     * Transitions the affiliate commission after the host charge succeeds.
     * <p>
     * Business rule: the platform never pays out what it has not collected.
     * Once the host charge succeeds, the affiliate commission advances —
     * but the next state depends on whether the affiliate has a fully onboarded
     * Stripe Connect account:
     * </p>
     * <ul>
     *   <li>Affiliate HAS a complete Connect account (canReceivePayouts == true):
     *       WAITING_HOST_CHARGE → APPROVED — ready to be paid on the next scheduled date.</li>
     *   <li>Affiliate does NOT have a Connect account or onboarding is incomplete:
     *       WAITING_HOST_CHARGE → PENDING — waits until the affiliate completes onboarding.
     *       A scheduler or manual admin action will move PENDING → APPROVED once the
     *       affiliate finishes Stripe Connect onboarding.</li>
     * </ul>
     *
     * @param bookingId the booking ID to find the associated affiliate commission
     */
    private void approveAffiliateCommissionIfWaiting(Long bookingId) {
        affiliateCommissionRepository.findByBookingId(bookingId)
            .ifPresent(affiliateCommission -> {
                if (affiliateCommission.getStatus() != ReferralCommissionStatus.WAITING_HOST_CHARGE) {
                    logger.debug("Affiliate commission for booking {} is in status {}, no transition needed",
                        bookingId, affiliateCommission.getStatus());
                    return;
                }

                StripeConnectAccount connectAccount = connectAccountRepository
                    .findByUserId(affiliateCommission.getAffiliateId()).orElse(null);

                boolean canReceivePayouts = connectAccount != null && connectAccount.canReceivePayouts();

                if (canReceivePayouts) {
                    affiliateCommission.approveAfterHostCharged();
                    affiliateCommissionRepository.save(affiliateCommission);
                    logger.info(
                        "Affiliate commission ID: {} APPROVED after host charge success for booking ID: {}. " +
                        "Affiliate {} has a fully onboarded Stripe Connect account.",
                        affiliateCommission.getId(), bookingId, affiliateCommission.getAffiliateId());
                } else {
                    affiliateCommission.pendingAfterHostCharge();
                    affiliateCommissionRepository.save(affiliateCommission);
                    logger.info(
                        "Affiliate commission ID: {} set to PENDING after host charge for booking ID: {}. " +
                        "Affiliate {} has no complete Stripe Connect account — commission will be paid once onboarding is done.",
                        affiliateCommission.getId(), bookingId, affiliateCommission.getAffiliateId());
                }
            });
    }

}
