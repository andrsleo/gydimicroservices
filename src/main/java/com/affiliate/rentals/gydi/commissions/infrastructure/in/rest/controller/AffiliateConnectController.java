package com.affiliate.rentals.gydi.commissions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.commissions.application.dto.ConnectAccountStatusDto;
import com.affiliate.rentals.gydi.commissions.application.dto.OnboardingLinkDto;
import com.affiliate.rentals.gydi.commissions.application.usecase.GetConnectAccountStatusUseCase;
import com.affiliate.rentals.gydi.commissions.application.usecase.InitiateAffiliateOnboardingUseCase;
import com.affiliate.rentals.gydi.commissions.domain.exception.AlreadyOnboardedException;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Stripe Connect affiliate onboarding.
 * <p>
 * FASE 3: Allows affiliates to connect their bank accounts for payouts.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/affiliates/connect")
@Tag(name = "Affiliate Connect", description = "Stripe Connect onboarding for affiliates")
@RequiredArgsConstructor
public class AffiliateConnectController {

    private static final Logger logger = LoggerFactory.getLogger(AffiliateConnectController.class);

    private final InitiateAffiliateOnboardingUseCase initiateOnboardingUseCase;
    private final GetConnectAccountStatusUseCase getConnectAccountStatusUseCase;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;
    private final PaymentGatewayPort paymentGateway;

    /**
     * Initiates Stripe Connect onboarding for current user.
     * <p>
     * Returns an onboarding URL that frontend should redirect user to.
     * User completes form on Stripe-hosted page, then returns via return_url.
     * </p>
     *
     * @return OnboardingLinkDto with URL to redirect to
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('USER') or hasRole('AFFILIATE') or hasRole('ADMIN')")
    @Operation(
        summary = "Initiate affiliate onboarding",
        description = "Creates Stripe Connect Account and returns onboarding link. User should be redirected to this URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Onboarding link generated successfully"),
        @ApiResponse(responseCode = "400", description = "Already onboarded"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> initiateOnboarding(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        logger.info("Initiating Stripe Connect onboarding for user ID: {}", userId);

        try {
            OnboardingLinkDto onboardingLink = initiateOnboardingUseCase.execute(userId);

            logger.info("Onboarding link generated for user {}: {}", userId, onboardingLink.stripeAccountId());

            return ResponseEntity.ok(onboardingLink);

        } catch (AlreadyOnboardedException e) {
            logger.warn("User {} already completed onboarding", userId);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ALREADY_ONBOARDED", e.getMessage()));

        } catch (Exception e) {
            logger.error("Error initiating onboarding for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("ONBOARDING_ERROR", "Failed to initiate onboarding: " + e.getMessage()));
        }
    }

    /**
     * Gets Stripe Connect Account status for current user.
     * <p>
     * Returns whether user has account, onboarding status, and payout capabilities.
     * Frontend uses this to show banners and onboarding CTAs.
     * </p>
     *
     * @return ConnectAccountStatusDto with account status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') or hasRole('AFFILIATE') or hasRole('ADMIN')")
    @Operation(
        summary = "Get Connect Account status",
        description = "Returns whether user has Stripe Connect Account and its onboarding/payout status"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ConnectAccountStatusDto> getAccountStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        logger.debug("Getting Connect Account status for user ID: {}", userId);

        ConnectAccountStatusDto status = getConnectAccountStatusUseCase.execute(userId);

        return ResponseEntity.ok(status);
    }

    /**
     * Generates a Stripe Express Dashboard login link for the current user.
     * <p>
     * The link is single-use and short-lived (~5 minutes). The user is redirected
     * to their Stripe Express Dashboard where they can manage bank accounts,
     * view payouts, and review transaction history.
     * </p>
     *
     * @return DashboardLinkDto with the Stripe Express Dashboard URL
     */
    @PostMapping("/dashboard-link")
    @PreAuthorize("hasRole('USER') or hasRole('AFFILIATE') or hasRole('ADMIN')")
    @Operation(
        summary = "Get Stripe Express Dashboard link",
        description = "Generates a single-use login link to the user's Stripe Express Dashboard"
    )
    public ResponseEntity<?> getDashboardLink(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        try {
            StripeConnectAccount account = connectAccountRepository.findByUserId(userId)
                .orElse(null);

            if (account == null || !account.isOnboardingCompleted()
                    || account.getStripeAccountId().startsWith("pending_")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("NOT_ONBOARDED",
                        "Complete Stripe Connect onboarding before accessing the dashboard"));
            }

            String url = paymentGateway.createLoginLink(account.getStripeAccountId());
            return ResponseEntity.ok(new DashboardLinkDto(url));

        } catch (Exception e) {
            logger.error("Error generating dashboard link for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("DASHBOARD_LINK_ERROR",
                    "Failed to generate dashboard link: " + e.getMessage()));
        }
    }

    /**
     * Error response record.
     */
    private record ErrorResponse(String code, String message) {}

    /**
     * Dashboard link response record.
     */
    private record DashboardLinkDto(String url) {}
}
