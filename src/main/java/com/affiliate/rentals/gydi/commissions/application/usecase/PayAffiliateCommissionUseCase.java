package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PayoutGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.ReferralCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Use Case: Transfers affiliate commission via PayPal Payouts API.
 * <p>
 * Business Flow:
 * 1. Retrieve ReferralCommission (must be APPROVED)
 * 2. Validate affiliate has a PayPal email configured
 * 3. Send payout via PayoutGatewayPort (PayPal Payouts API in prod, mock in dev)
 * 4. Mark commission as PAID with PayPal batch/item IDs
 * </p>
 * <p>
 * If the affiliate has not configured their PayPal email, the payout
 * is skipped and logged. The commission remains APPROVED and will be
 * retried by the scheduler on the 1st and 15th of each month once
 * the affiliate configures their PayPal email.
 * </p>
 * <p>
 * If the payout fails: the commission stays APPROVED (not moved to FAILED)
 * so the scheduler will retry. After 3 consecutive failures it transitions
 * to WITHHELD for manual review.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PayAffiliateCommissionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PayAffiliateCommissionUseCase.class);

    private final ReferralCommissionRepositoryPort commissionRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PayoutGatewayPort payoutGateway;
    private final UserRepositoryPort userRepository;
    private final BookingRepositoryPort bookingRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    /**
     * Executes the affiliate commission payout.
     *
     * @param affiliateCommissionId ID of the affiliate commission to pay
     * @throws CommissionNotFoundException if commission not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long affiliateCommissionId) {
        ReferralCommission commission = commissionRepository.findById(affiliateCommissionId)
                .orElseThrow(() -> CommissionNotFoundException.forId(affiliateCommissionId));

        if (commission.getStatus() != ReferralCommissionStatus.APPROVED) {
            logger.debug("Commission {} is not APPROVED (current: {}), skipping payout",
                    affiliateCommissionId, commission.getStatus());
            return;
        }

        // Resolve affiliate's PayPal email from their account record
        StripeConnectAccount account = connectAccountRepository
                .findByUserId(commission.getAffiliateId()).orElse(null);

        if (account == null || !account.hasPayPalEmail()) {
            logger.warn("Affiliate {} has no PayPal email configured — skipping payout for commission {}. " +
                    "Commission will be retried once affiliate registers their PayPal email.",
                    commission.getAffiliateId(), affiliateCommissionId);
            // Leave commission in APPROVED — scheduler will retry
            return;
        }

        BigDecimal amount = commission.getAmount().getCommissionAmount();
        String currency = commission.getAmount().getCurrency();
        String note = buildPayoutNote(commission);

        try {
            PayoutGatewayPort.PayoutResult result = payoutGateway.sendPayout(
                    account.getPaypalEmail(),
                    amount,
                    currency,
                    affiliateCommissionId.toString(),
                    note
            );

            if (result.success()) {
                commission.markAsPaid(result.payoutBatchId(), result.payoutItemId());
                commissionRepository.save(commission);
                logger.info("Affiliate commission {} paid via PayPal: batchId={}, itemId={}",
                        affiliateCommissionId, result.payoutBatchId(), result.payoutItemId());

                // Fire-and-forget: send commission earned email to affiliate
                sendAffiliateCommissionEarnedEmail(commission);

            } else {
                commission.recordPaymentFailure(result.failureReason());
                commissionRepository.save(commission);
                logger.warn("PayPal payout failed for commission {}: {}",
                        affiliateCommissionId, result.failureReason());
            }

        } catch (Exception e) {
            logger.error("Exception while paying affiliate commission {}", affiliateCommissionId, e);
            // Leave commission in APPROVED on unexpected errors so scheduler can retry
            // Only persist the failure if the domain allows it (status is still APPROVED)
            try {
                commission.recordPaymentFailure("System error: " + e.getMessage());
                commissionRepository.save(commission);
            } catch (Exception inner) {
                logger.error("Could not persist failure for commission {}: {}",
                        affiliateCommissionId, inner.getMessage());
            }
        }
    }

    // ============================================================================
    // PRIVATE HELPERS
    // ============================================================================

    private String buildPayoutNote(ReferralCommission commission) {
        return String.format("Comision GYDI - reserva #%d - %s %.2f %s",
                commission.getBookingId(),
                commission.getAffiliatePlan(),
                commission.getAmount().getCommissionAmount(),
                commission.getAmount().getCurrency());
    }

    /**
     * Sends an affiliate-commission-earned email. Fire-and-forget: never throws.
     */
    private void sendAffiliateCommissionEarnedEmail(ReferralCommission commission) {
        try {
            userRepository.findById(commission.getAffiliateId()).ifPresent(affiliate -> {
                // Resolve property title via booking -> property chain
                String propertyTitle = bookingRepository.findById(commission.getBookingId())
                        .flatMap(booking -> propertyRepository.findById(PropertyId.of(booking.getPropertyId())))
                        .map(property -> property.getTitle())
                        .orElse("una propiedad");

                EmailMessage email = emailTemplateService.buildAffiliateCommissionEarnedEmail(
                        affiliate.email().address(),
                        new EmailTemplateService.AffiliateCommissionEarnedEmailData(
                                affiliate.name(),
                                commission.getAmount().getCommissionAmount(),
                                commission.getAmount().getCurrency(),
                                "un huesped referido",
                                propertyTitle
                        )
                );
                emailService.sendEmail(email);
                logger.info("Affiliate commission earned email sent to affiliate {} for commission {}",
                        commission.getAffiliateId(), commission.getId());
            });
        } catch (Exception e) {
            logger.error("Failed to send affiliate commission earned email for commission {}: {}",
                    commission.getId(), e.getMessage());
        }
    }
}
