package com.affiliate.rentals.gydi.commissions.infrastructure.out.payment;

import com.affiliate.rentals.gydi.commissions.domain.ports.PaymentGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Stripe Payment Gateway Adapter for host commission processing.
 * <p>
 * Handles HOST commission charges via Stripe Payment Intents (off_session=true).
 * </p>
 * <p>
 * Note: Stripe Connect affiliate payout methods have been removed. GYDI now uses
 * PayPal Payouts API for affiliate commission payments (see PayPalPayoutAdapter).
 * The stripe_connect_accounts table is still used to store:
 *   - stripe_platform_customer_id (cus_xxx) for charging host commissions
 *   - paypal_email for sending affiliate commission payouts
 * </p>
 */
@Component("commissionStripePaymentGatewayAdapter")
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);

    @Value("${stripe.api-key:}")
    private String stripeSecretKey;

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
            com.stripe.Stripe.apiKey = stripeSecretKey;

            Map<String, String> metadata = new HashMap<>();
            metadata.put("booking_id", bookingId);
            metadata.put("commission_id", commissionId);
            metadata.put("type", "host_commission");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setOffSession(true)
                    .setConfirm(true)
                    .putAllMetadata(metadata)
                    .setDescription("Host commission for booking #" + bookingId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            logger.info("Host commission charged successfully. PaymentIntent: {}, Charge: {}, Status: {}",
                    intent.getId(), intent.getLatestCharge(), intent.getStatus());

            return PaymentResult.success(intent.getId(), intent.getLatestCharge());

        } catch (StripeException e) {
            logger.error("Failed to charge host commission: {}", e.getMessage(), e);
            return PaymentResult.failure(formatStripeError(e));
        }
    }

    @Override
    public PaymentResult chargeHostViaConnect(
            String hostConnectAccountId,
            String customerId,
            String paymentMethodId,
            Long totalAmountCents,
            Long applicationFeeCents,
            String currency,
            String bookingId,
            String commissionId
    ) {
        logger.info("Charging host via Connect: totalAmount={} cents, appFee={} cents, hostAccount={}, booking={}, commission={}",
                totalAmountCents, applicationFeeCents, hostConnectAccountId, bookingId, commissionId);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            Map<String, String> metadata = new HashMap<>();
            metadata.put("booking_id", bookingId);
            metadata.put("commission_id", commissionId);
            metadata.put("type", "host_commission");
            metadata.put("connect_model", "true");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalAmountCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setOffSession(true)
                    .setConfirm(true)
                    .setApplicationFeeAmount(applicationFeeCents)
                    .setOnBehalfOf(hostConnectAccountId)
                    .setTransferData(
                            PaymentIntentCreateParams.TransferData.builder()
                                    .setDestination(hostConnectAccountId)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .setDescription("Host booking commission for booking #" + bookingId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            logger.info("Host Connect charge successful. PaymentIntent: {}, Charge: {}, Status: {}",
                    intent.getId(), intent.getLatestCharge(), intent.getStatus());

            return PaymentResult.success(intent.getId(), intent.getLatestCharge());

        } catch (StripeException e) {
            logger.error("Failed to charge host via Connect: {}", e.getMessage(), e);
            return PaymentResult.failure(formatStripeError(e));
        }
    }

    @Override
    public PaymentResult refundHostCommission(String chargeId, Long amountCents, String reason) {
        logger.info("Refunding charge: {}, amount: {} cents, reason: {}", chargeId, amountCents, reason);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

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
    // PAYMENT METHOD VERIFICATION
    // ============================================================================

    @Override
    public boolean verifyPaymentMethod(String customerId, String paymentMethodId) {
        logger.info("Verifying payment method: {} for customer: {}", paymentMethodId, customerId);

        try {
            com.stripe.Stripe.apiKey = stripeSecretKey;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(100L)
                    .setCurrency("usd")
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setDescription("Payment method verification")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            boolean verified = "requires_capture".equals(intent.getStatus());

            if (verified) {
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
    // PRIVATE HELPER METHODS
    // ============================================================================

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
