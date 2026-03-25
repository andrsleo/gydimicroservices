package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.ConnectAccountStatusDto;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Gets payout account status for a user.
 * <p>
 * Returns whether the user has a payout account record, their PayPal email
 * configuration status, and legacy Stripe Connect fields for backward compatibility.
 * </p>
 * <p>
 * Note: The Stripe Connect lazy-sync logic has been removed as GYDI now uses
 * PayPal Payouts for affiliate commissions. Account status is read entirely from
 * the local database without making any external API calls.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetConnectAccountStatusUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetConnectAccountStatusUseCase.class);

    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    /**
     * Returns the payout account status for the given user.
     *
     * @param userId ID of the user
     * @return ConnectAccountStatusDto with account and PayPal configuration status
     */
    @Transactional(readOnly = true)
    public ConnectAccountStatusDto execute(Long userId) {
        logger.debug("Getting payout account status for user ID: {}", userId);

        var account = connectAccountRepository.findByUserId(userId);

        if (account.isEmpty()) {
            return ConnectAccountStatusDto.notFound();
        }

        StripeConnectAccount connectAccount = account.get();

        if (connectAccount.hasPayPalEmail()) {
            return ConnectAccountStatusDto.withPayPal(
                    connectAccount.isOnboardingCompleted(),
                    connectAccount.isPayoutsEnabled(),
                    connectAccount.getVerificationStatus(),
                    connectAccount.getStripeAccountId(),
                    connectAccount.getPaypalEmail()
            );
        }

        return ConnectAccountStatusDto.withoutPayPal(
                connectAccount.isOnboardingCompleted(),
                connectAccount.isPayoutsEnabled(),
                connectAccount.getVerificationStatus(),
                connectAccount.getStripeAccountId()
        );
    }
}
