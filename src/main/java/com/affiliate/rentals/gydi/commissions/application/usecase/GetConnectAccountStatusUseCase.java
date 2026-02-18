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
 * Use Case: Gets Connect Account status for a user.
 * <p>
 * Returns whether user has account, onboarding status, payout capabilities.
 * Used by frontend to show banners and onboarding CTAs.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetConnectAccountStatusUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetConnectAccountStatusUseCase.class);

    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    /**
     * Executes the status check.
     *
     * @param userId ID of the user
     * @return ConnectAccountStatusDto with account status
     */
    @Transactional(readOnly = true)
    public ConnectAccountStatusDto execute(Long userId) {
        logger.debug("Getting Connect Account status for user ID: {}", userId);

        var account = connectAccountRepository.findByUserId(userId);

        if (account.isEmpty()) {
            return ConnectAccountStatusDto.notFound();
        }

        StripeConnectAccount connectAccount = account.get();

        return new ConnectAccountStatusDto(
            true,
            connectAccount.isOnboardingCompleted(),
            connectAccount.isPayoutsEnabled(),
            connectAccount.getVerificationStatus(),
            connectAccount.getStripeAccountId()
        );
    }
}
