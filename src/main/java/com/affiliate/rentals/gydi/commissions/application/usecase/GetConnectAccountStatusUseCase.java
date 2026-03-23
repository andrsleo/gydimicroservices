package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.ConnectAccountStatusDto;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Gets Connect Account status for a user.
 * <p>
 * Returns whether user has account, onboarding status, payout capabilities.
 * Used by frontend to show banners and onboarding CTAs.
 * </p>
 * <p>
 * Lazy sync: when local status is incomplete but a real Stripe account exists
 * (acct_xxx, not pending_N), this use case queries Stripe directly and persists
 * the updated status. This ensures correctness even when the account.updated
 * webhook was missed (e.g., Stripe CLI not running during testing).
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetConnectAccountStatusUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetConnectAccountStatusUseCase.class);

    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PaymentGatewayPort paymentGateway;

    /**
     * Executes the status check.
     * <p>
     * If the local record says onboarding is incomplete but the account has a real
     * Stripe ID (not a stub), queries Stripe for the live status and syncs it to the DB.
     * </p>
     *
     * @param userId ID of the user
     * @return ConnectAccountStatusDto with account status
     */
    @Transactional
    public ConnectAccountStatusDto execute(Long userId) {
        logger.debug("Getting Connect Account status for user ID: {}", userId);

        var account = connectAccountRepository.findByUserId(userId);

        if (account.isEmpty()) {
            return ConnectAccountStatusDto.notFound();
        }

        StripeConnectAccount connectAccount = account.get();

        // Lazy sync: local status incomplete + real Stripe account → query Stripe
        if (!connectAccount.isOnboardingCompleted()
                && !connectAccount.getStripeAccountId().startsWith("pending_")) {
            connectAccount = syncFromStripe(connectAccount);
        }

        return new ConnectAccountStatusDto(
            true,
            connectAccount.isOnboardingCompleted(),
            connectAccount.isPayoutsEnabled(),
            connectAccount.getVerificationStatus(),
            connectAccount.getStripeAccountId()
        );
    }

    /**
     * Queries Stripe for the latest account status and updates our DB if anything changed.
     *
     * @param account the local Connect account record
     * @return the (possibly updated) domain object
     */
    private StripeConnectAccount syncFromStripe(StripeConnectAccount account) {
        try {
            PaymentGatewayPort.ConnectAccountStatus stripeStatus =
                    paymentGateway.getConnectAccountStatus(account.getStripeAccountId());

            boolean changed = stripeStatus.onboardingCompleted() != account.isOnboardingCompleted()
                    || stripeStatus.payoutsEnabled() != account.isPayoutsEnabled()
                    || stripeStatus.chargesEnabled() != account.isChargesEnabled();

            if (changed) {
                logger.info("Syncing Connect account {} from Stripe: detailsSubmitted={}, payoutsEnabled={}, chargesEnabled={}",
                        account.getStripeAccountId(),
                        stripeStatus.onboardingCompleted(),
                        stripeStatus.payoutsEnabled(),
                        stripeStatus.chargesEnabled());

                account.updateFromStripeWebhook(
                        stripeStatus.onboardingCompleted(),
                        stripeStatus.payoutsEnabled(),
                        stripeStatus.chargesEnabled()
                );
                account = connectAccountRepository.save(account);
            }
        } catch (Exception e) {
            // Log and return local data — don't fail the status check
            logger.warn("Failed to sync Connect account {} from Stripe: {}",
                    account.getStripeAccountId(), e.getMessage());
        }
        return account;
    }
}
