package com.affiliate.rentals.gydi.commissions.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.commissions.application.dto.ConnectAccountStatusDto;
import com.affiliate.rentals.gydi.commissions.application.usecase.GetConnectAccountStatusUseCase;
import com.affiliate.rentals.gydi.commissions.application.usecase.SavePayPalEmailUseCase;
import com.affiliate.rentals.gydi.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for affiliate payout configuration.
 * <p>
 * Replaces the former Stripe Connect onboarding endpoints.
 * Affiliates now register their PayPal email to receive commission payouts.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/affiliates")
@Tag(name = "Affiliate Payouts", description = "Affiliate commission payout configuration (PayPal)")
@RequiredArgsConstructor
public class AffiliateConnectController {

    private static final Logger logger = LoggerFactory.getLogger(AffiliateConnectController.class);

    private final SavePayPalEmailUseCase savePayPalEmailUseCase;
    private final GetConnectAccountStatusUseCase getConnectAccountStatusUseCase;

    /**
     * Registers or updates the affiliate's PayPal email for commission payouts.
     * <p>
     * Replaces the former Stripe Connect /initiate and /dashboard-link endpoints.
     * Once the email is saved, GYDI will send commission payouts to this address
     * via the PayPal Payouts API on the 1st and 15th of each month.
     * </p>
     */
    @PutMapping("/payout/paypal-email")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Register PayPal email for commission payouts",
        description = "Saves the affiliate's PayPal email. Commission payouts will be sent to this address via PayPal Payouts API."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PayPal email saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> savePayPalEmail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody PayPalEmailRequest request
    ) {
        Long userId = userDetails.getUserId();
        logger.info("Saving PayPal email for user ID: {}", userId);

        try {
            savePayPalEmailUseCase.execute(userId, request.paypalEmail());
            return ResponseEntity.ok(new SuccessResponse("PayPal email registered successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid PayPal email for user {}: {}", userId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_EMAIL", e.getMessage()));

        } catch (Exception e) {
            logger.error("Error saving PayPal email for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SAVE_ERROR", "Failed to save PayPal email: " + e.getMessage()));
        }
    }

    /**
     * Returns the affiliate's payout account status including PayPal email configuration.
     */
    @GetMapping("/payout/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get payout account status",
        description = "Returns the affiliate's payout configuration status including whether a PayPal email is registered."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ConnectAccountStatusDto> getPayoutStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        logger.debug("Getting payout status for user ID: {}", userId);

        ConnectAccountStatusDto status = getConnectAccountStatusUseCase.execute(userId);
        return ResponseEntity.ok(status);
    }

    // ============================================================================
    // REQUEST / RESPONSE RECORDS
    // ============================================================================

    /**
     * Request body for saving PayPal email.
     */
    public record PayPalEmailRequest(
        @NotBlank(message = "PayPal email is required")
        @Email(message = "Invalid PayPal email format")
        String paypalEmail
    ) {}

    private record SuccessResponse(String message) {}

    private record ErrorResponse(String code, String message) {}
}
