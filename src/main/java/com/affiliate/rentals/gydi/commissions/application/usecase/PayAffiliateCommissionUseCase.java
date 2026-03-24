package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.exception.CommissionNotFoundException;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
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
import java.time.LocalDateTime;

/**
 * Use Case: Transfers affiliate commission via Stripe Connect.
 * <p>
 * Business Flow:
 * 1. Retrieve ReferralCommission (must be APPROVED)
 * 2. Validate affiliate has a fully onboarded Stripe Connect account
 * 3. Transfer commission amount to affiliate's acct_xxx
 * 4. Mark commission as PAID
 * </p>
 * <p>
 * If the affiliate has not completed Stripe Connect onboarding, the payment
 * is skipped. It will be retried by the scheduler on the 1st and 15th of
 * each month once onboarding is complete.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PayAffiliateCommissionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PayAffiliateCommissionUseCase.class);

    private final ReferralCommissionRepositoryPort commissionRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PaymentGatewayPort paymentGateway;
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

        StripeConnectAccount connectAccount = connectAccountRepository
                .findByUserId(commission.getAffiliateId()).orElse(null);

        if (connectAccount == null || !connectAccount.canReceivePayouts()) {
            logger.warn("Affiliate {} cannot receive payouts — onboarding incomplete or not started",
                    commission.getAffiliateId());
            // Will be retried by scheduler once onboarding is complete
            return;
        }

        long amountCents = commission.getAmount().getCommissionAmount()
                .multiply(new BigDecimal("100"))
                .longValue();

        try {
            PaymentGatewayPort.PaymentResult result = paymentGateway.transferToAffiliate(
                    connectAccount.getStripeAccountId(),  // acct_xxx
                    amountCents,
                    commission.getAmount().getCurrency(),
                    affiliateCommissionId.toString()
            );

            if (result.success()) {
                commission.markAsPaid(result.transactionId(), null);
                commissionRepository.save(commission);
                logger.info("Affiliate commission {} paid via Stripe Transfer {}",
                        affiliateCommissionId, result.transactionId());

                // Fire-and-forget: send commission earned email to affiliate
                sendAffiliateCommissionEarnedEmail(commission);

            } else {
                commission.recordPaymentFailure(result.failureReason());
                commissionRepository.save(commission);
                logger.warn("Affiliate payout failed for commission {}: {}",
                        affiliateCommissionId, result.failureReason());
            }
        } catch (Exception e) {
            logger.error("Exception while paying affiliate commission {}", affiliateCommissionId, e);
            commission.recordPaymentFailure("System error: " + e.getMessage());
            commissionRepository.save(commission);
        }
    }

    /**
     * Sends an affiliate-commission-earned email. Fire-and-forget: never throws.
     */
    private void sendAffiliateCommissionEarnedEmail(ReferralCommission commission) {
        try {
            userRepository.findById(commission.getAffiliateId()).ifPresent(affiliate -> {
                // Resolve property title via booking → property chain
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
                                "un huésped referido",
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
