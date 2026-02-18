package com.affiliate.rentals.gydi.commissions.infrastructure.out.payment;

import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.AccountRetrieveParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.TransferCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe Payment Gateway Adapter for commission processing.
 * <p>
 * Handles:
 * - HOST commission charges via Payment Intents (off_session=true)
 * - AFFILIATE payouts via Stripe Connect Transfers
 * - Connect account creation and onboarding
 * - Payment method verification
 * </p>
 * <p>
 * Uses Stripe Java SDK v26.3.0
 * </p>
 */
@Component("commissionStripePaymentGatewayAdapter")
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);

    @Value("${stripe.api-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    // ============================================================================
    // HOST COMMISSION CHARGES (Payment Intents)
    // ============================================================================

    @Override
    public PaymentResult chargeHostCommission(
            String customerId,
            String paymentMethodId,
            Long amountCents,
            String currency,
            String bookingId,
            String commissionId
    ) {
        logger.info("Charging host commission: {} cents ({}) for customer: {}, booking: {}, commission: {}",
                amountCents, currency, customerId, bookingId, commissionId);

        try {
            // Initialize Stripe API key
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Build metadata for tracking
            Map<String, String> metadata = new HashMap<>();
            metadata.put("booking_id", bookingId);
            metadata.put("commission_id", commissionId);
            metadata.put("type", "host_commission");

            // Create Payment Intent with off_session=true (backend-initiated charge)
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setOffSession(true)  // Critical: allows backend-initiated charges
                    .setConfirm(true)      // Auto-confirm the payment intent
                    .putAllMetadata(metadata)
                    .setDescription("Host commission for booking #" + bookingId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            logger.info("Host commission charged successfully. PaymentIntent: {}, Charge: {}, Status: {}",
                    intent.getId(),
                    intent.getLatestCharge(),
                    intent.getStatus());

            return PaymentResult.success(intent.getId(), intent.getLatestCharge());

        } catch (StripeException e) {
            logger.error("Failed to charge host commission: {}", e.getMessage(), e);
            return PaymentResult.failure(formatStripeError(e));
        }
    }

    @Override
    public PaymentResult refundHostCommission(String chargeId, Long amountCents, String reason) {
        logger.info("Refunding charge: {}, amount: {} cents, reason: {}", chargeId, amountCents, reason);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Create refund
            Map<String, Object> refundParams = new HashMap<>();
            refundParams.put("charge", chargeId);
            refundParams.put("amount", amountCents);
            refundParams.put("reason", reason);

            com.stripe.model.Refund refund = com.stripe.model.Refund.create(refundParams);

            logger.info("Refund successful. Refund ID: {}, Status: {}", refund.getId(), refund.getStatus());

            return PaymentResult.success(refund.getId(), null);

        } catch (StripeException e) {
            logger.error("Failed to refund charge: {}", e.getMessage(), e);
            return PaymentResult.failure(formatStripeError(e));
        }
    }

    // ============================================================================
    // AFFILIATE PAYOUTS (Stripe Connect)
    // ============================================================================

    @Override
    public String createConnectAccount(String email, String country) {
        logger.info("Creating Stripe Connect account for email: {}, country: {}", email, country);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Determine account type based on country
            AccountCreateParams.Type accountType = determineAccountType(country);

            // Build account creation params
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(accountType)
                    .setCountry(country.toUpperCase())
                    .setEmail(email)
                    .setCapabilities(buildCapabilities(country))
                    .build();

            Account account = Account.create(params);

            logger.info("Stripe Connect account created: {}, type: {}, country: {}",
                    account.getId(), account.getType(), account.getCountry());

            return account.getId();

        } catch (StripeException e) {
            logger.error("Failed to create Connect account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Stripe Connect account: " + formatStripeError(e), e);
        }
    }

    @Override
    public String generateConnectOnboardingLink(String accountId, String refreshUrl, String returnUrl) {
        logger.info("Generating Connect onboarding link for account: {}", accountId);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink link = AccountLink.create(params);

            logger.info("Onboarding link generated: {} (expires at: {})", link.getUrl(), link.getExpiresAt());

            return link.getUrl();

        } catch (StripeException e) {
            logger.error("Failed to generate onboarding link: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate onboarding link: " + formatStripeError(e), e);
        }
    }

    @Override
    public PaymentResult transferToAffiliate(
            String connectAccountId,
            Long amountCents,
            String currency,
            String commissionIds
    ) {
        logger.info("Transferring {} cents ({}) to affiliate Connect account: {}, commissions: {}",
                amountCents, currency, connectAccountId, commissionIds);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("commission_ids", commissionIds);
            metadata.put("type", "affiliate_payout");
            metadata.put("payout_date", LocalDateTime.now().toString());

            // Create Transfer
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency.toLowerCase())
                    .setDestination(connectAccountId)
                    .putAllMetadata(metadata)
                    .setDescription("Affiliate commission payout - " + commissionIds)
                    .build();

            Transfer transfer = Transfer.create(params);

            logger.info("Transfer successful. Transfer ID: {}, Status: {}, Arrival Date: {}",
                    transfer.getId(),
                    transfer.getObject(),
                    transfer.getCreated());

            return PaymentResult.success(transfer.getId(), null);

        } catch (StripeException e) {
            logger.error("Failed to transfer to affiliate: {}", e.getMessage(), e);
            return PaymentResult.failure(formatStripeError(e));
        }
    }

    // ============================================================================
    // PAYMENT METHOD VERIFICATION
    // ============================================================================

    @Override
    public boolean verifyPaymentMethod(String customerId, String paymentMethodId) {
        logger.info("Verifying payment method: {} for customer: {}", paymentMethodId, customerId);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Create $1 authorization (100 cents) - not captured
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(100L)  // $1.00 in cents
                    .setCurrency("usd")
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)  // Authorization only, don't capture
                    .setDescription("Payment method verification")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // If status is requires_capture, verification succeeded
            boolean verified = "requires_capture".equals(intent.getStatus());

            if (verified) {
                // Cancel the authorization (release the hold)
                intent.cancel();
                logger.info("Payment method verified successfully and authorization released");
            } else {
                logger.warn("Payment method verification failed. Status: {}", intent.getStatus());
            }

            return verified;

        } catch (StripeException e) {
            logger.error("Failed to verify payment method: {}", e.getMessage(), e);
            return false;
        }
    }

    // ============================================================================
    // CONNECT ACCOUNT QUERIES
    // ============================================================================

    @Override
    public ConnectAccountStatus getConnectAccountStatus(String accountId) {
        logger.info("Retrieving Connect account status for: {}", accountId);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            AccountRetrieveParams params = AccountRetrieveParams.builder().build();
            Account account = Account.retrieve(accountId, params, null);

            boolean onboardingCompleted = account.getDetailsSubmitted() != null && account.getDetailsSubmitted();
            boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();
            boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();

            String disabledReason = null;
            if (!payoutsEnabled && account.getRequirements() != null) {
                disabledReason = account.getRequirements().getDisabledReason();
            }

            logger.info("Connect account status: onboarding={}, payouts={}, charges={}, disabled_reason={}",
                    onboardingCompleted, payoutsEnabled, chargesEnabled, disabledReason);

            return new ConnectAccountStatus(
                    accountId,
                    onboardingCompleted,
                    payoutsEnabled,
                    chargesEnabled,
                    disabledReason
            );

        } catch (StripeException e) {
            logger.error("Failed to retrieve Connect account status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve Connect account status: " + formatStripeError(e), e);
        }
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Determines Stripe Connect account type based on country.
     * <p>
     * - US, CA, UK: Standard (full control, separate Stripe dashboard)
     * - EU countries: Express (streamlined onboarding, shared dashboard)
     * - Other: Custom (most flexible, requires more integration work)
     * </p>
     */
    private AccountCreateParams.Type determineAccountType(String country) {
        return switch (country.toUpperCase()) {
            case "US", "CA", "GB" -> AccountCreateParams.Type.STANDARD;
            case "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                 "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
                 "PL", "PT", "RO", "SK", "SI", "ES", "SE" -> AccountCreateParams.Type.EXPRESS;
            default -> AccountCreateParams.Type.CUSTOM;
        };
    }

    /**
     * Builds capabilities for Stripe Connect account based on country.
     * <p>
     * Capabilities determine what the Connect account can do (transfers, card payments, etc.)
     * </p>
     */
    private AccountCreateParams.Capabilities buildCapabilities(String country) {
        AccountCreateParams.Capabilities.Builder builder = AccountCreateParams.Capabilities.builder()
                .setTransfers(
                        AccountCreateParams.Capabilities.Transfers.builder()
                                .setRequested(true)
                                .build()
                );

        // EU countries support card payments capability
        if (isSepaCountry(country)) {
            builder.setCardPayments(
                    AccountCreateParams.Capabilities.CardPayments.builder()
                            .setRequested(true)
                            .build()
            );
        }

        return builder.build();
    }

    /**
     * Checks if country is in SEPA zone (Single Euro Payments Area).
     */
    private boolean isSepaCountry(String country) {
        String upperCountry = country.toUpperCase();
        return upperCountry.equals("AT") || upperCountry.equals("BE") || upperCountry.equals("BG") ||
                upperCountry.equals("HR") || upperCountry.equals("CY") || upperCountry.equals("CZ") ||
                upperCountry.equals("DK") || upperCountry.equals("EE") || upperCountry.equals("FI") ||
                upperCountry.equals("FR") || upperCountry.equals("DE") || upperCountry.equals("GR") ||
                upperCountry.equals("HU") || upperCountry.equals("IE") || upperCountry.equals("IT") ||
                upperCountry.equals("LV") || upperCountry.equals("LT") || upperCountry.equals("LU") ||
                upperCountry.equals("MT") || upperCountry.equals("NL") || upperCountry.equals("PL") ||
                upperCountry.equals("PT") || upperCountry.equals("RO") || upperCountry.equals("SK") ||
                upperCountry.equals("SI") || upperCountry.equals("ES") || upperCountry.equals("SE");
    }

    /**
     * Formats Stripe exception for user-friendly error messages.
     */
    private String formatStripeError(StripeException e) {
        if (e.getCode() != null) {
            return switch (e.getCode()) {
                case "insufficient_funds" -> "Insufficient funds in account";
                case "card_declined" -> "Card was declined";
                case "expired_card" -> "Card has expired";
                case "incorrect_cvc" -> "Incorrect CVC code";
                case "processing_error" -> "An error occurred while processing the card";
                case "rate_limit" -> "Too many requests, please try again later";
                default -> e.getUserMessage() != null ? e.getUserMessage() : e.getMessage();
            };
        }
        return e.getMessage();
    }
}
