package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.OnboardingLinkDto;
import com.affiliate.rentals.gydi.commissions.domain.exception.AlreadyOnboardedException;
import com.affiliate.rentals.gydi.commissions.domain.model.ConnectAccountType;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Initiates affiliate onboarding with Stripe Connect.
 * <p>
 * Business Flow:
 * 1. Check if user already has Connect Account
 * 2. If exists and onboarding complete → throw AlreadyOnboardedException
 * 3. If exists but incomplete → generate new onboarding link
 * 4. If not exists → create Connect Account + generate onboarding link
 * </p>
 * <p>
 * Account Type Selection:
 * - US, CA, UK → STANDARD
 * - EU countries → EXPRESS
 * - Others → CUSTOM
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InitiateAffiliateOnboardingUseCase {

    private static final Logger logger = LoggerFactory.getLogger(InitiateAffiliateOnboardingUseCase.class);

    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PaymentGatewayPort paymentGateway;
    private final UserRepositoryPort userRepository;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Executes the affiliate onboarding initiation.
     *
     * @param userId ID of the user initiating onboarding
     * @return OnboardingLinkDto with URL to redirect user
     * @throws AlreadyOnboardedException if user already completed onboarding
     */
    @Transactional
    public OnboardingLinkDto execute(Long userId) {
        logger.info("Initiating affiliate onboarding for user ID: {}", userId);

        // 1. Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Check if already has Connect Account
        var existingAccount = connectAccountRepository.findByUserId(userId);

        if (existingAccount.isPresent()) {
            StripeConnectAccount account = existingAccount.get();

            // If onboarding already complete, throw exception
            if (account.isOnboardingComplete() && account.isPayoutsEnabled()) {
                logger.warn("User {} already completed onboarding", userId);
                throw new AlreadyOnboardedException(
                    "User already completed Stripe Connect onboarding"
                );
            }

            // Onboarding incomplete → generate new link
            logger.info("User {} has incomplete onboarding, generating new link", userId);
            String onboardingUrl = generateOnboardingLink(account.getStripeAccountId());
            return OnboardingLinkDto.of(onboardingUrl, account.getStripeAccountId());
        }

        // 3. No account exists → create new Connect Account
        logger.info("Creating new Stripe Connect Account for user {}", userId);

        // Determine account type based on country
        String country = detectUserCountry(user);
        ConnectAccountType accountType = determineAccountType(country);

        // Create Stripe Connect Account
        String stripeAccountId = paymentGateway.createConnectAccount(
            user.email().address(),
            country
        );

        // Save to database
        StripeConnectAccount newAccount = StripeConnectAccount.create(
            userId,
            stripeAccountId,
            accountType,
            country
        );
        connectAccountRepository.save(newAccount);

        logger.info("Created Stripe Connect Account: {} for user {}", stripeAccountId, userId);

        // 4. Generate onboarding link
        String onboardingUrl = generateOnboardingLink(stripeAccountId);

        return OnboardingLinkDto.of(onboardingUrl, stripeAccountId);
    }

    /**
     * Generates Stripe onboarding link for the account.
     */
    private String generateOnboardingLink(String stripeAccountId) {
        String refreshUrl = baseUrl + "/dashboard/affiliate/setup";
        String returnUrl = baseUrl + "/dashboard/affiliate/success";

        return paymentGateway.generateConnectOnboardingLink(
            stripeAccountId,
            refreshUrl,
            returnUrl
        );
    }

    /**
     * Detects user's country.
     * <p>
     * Priority:
     * 1. User profile country (if set)
     * 2. Detected country from registration (IP-based)
     * 3. Default to "US"
     * </p>
     */
    private String detectUserCountry(User user) {
        // TODO: Implement country detection logic
        // For now, default to US
        // In production, this should:
        // - Check user.getCountry() if available
        // - Use GeoIP lookup if needed
        // - Comply with local regulations
        return "US";
    }

    /**
     * Determines Stripe Connect account type based on country.
     * <p>
     * Account Types:
     * - STANDARD: US, CA, UK, AU, NZ
     * - EXPRESS: EU countries (simplified onboarding)
     * - CUSTOM: Other countries (more control)
     * </p>
     */
    private ConnectAccountType determineAccountType(String country) {
        return switch (country.toUpperCase()) {
            case "US", "CA", "UK", "GB", "AU", "NZ" -> ConnectAccountType.STANDARD;
            case "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                 "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
                 "PL", "PT", "RO", "SK", "SI", "ES", "SE" -> ConnectAccountType.EXPRESS;
            default -> ConnectAccountType.CUSTOM;
        };
    }
}
