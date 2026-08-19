package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.domain.model.ConnectAccountType;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Saves (or updates) the affiliate's PayPal email for receiving commission payouts.
 * <p>
 * Replaces the Stripe Connect onboarding flow. Affiliates simply provide their
 * PayPal email address and GYDI will send commission payouts to it via the PayPal Payouts API.
 * </p>
 * <p>
 * If the affiliate does not yet have a {@code stripe_connect_accounts} record (used as the
 * unified payout account table), a stub record is created transparently.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SavePayPalEmailUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SavePayPalEmailUseCase.class);

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$";

    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final UserRepositoryPort userRepository;

    /**
     * Saves or updates the PayPal email for the given user.
     *
     * @param userId      ID of the authenticated user
     * @param paypalEmail the PayPal email address to register
     * @throws IllegalArgumentException if the user is not found or the email is invalid
     */
    @Transactional
    public void execute(Long userId, String paypalEmail) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate email format
        if (paypalEmail == null || paypalEmail.isBlank()) {
            throw new IllegalArgumentException("PayPal email cannot be empty");
        }
        String normalizedEmail = paypalEmail.trim().toLowerCase();
        if (!normalizedEmail.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid PayPal email format: " + paypalEmail);
        }

        // Delegate persistence to repository (creates stub account if needed)
        connectAccountRepository.savePayPalEmail(userId, normalizedEmail);

        logger.info("PayPal email saved for user {}: {}", userId, normalizedEmail);
    }
}
