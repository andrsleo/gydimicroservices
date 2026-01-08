package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.stripe;

import com.affiliate.rentals.gydi.subscriptions.domain.exception.PaymentFailedException;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe implementation of the PaymentGatewayPort.
 *
 * <p>This adapter provides integration with Stripe's payment processing API,
 * handling all payment operations including customers, payment methods,
 * subscriptions, and refunds.</p>
 *
 * <p><b>Security:</b> This adapter ensures PCI compliance by:</p>
 * <ul>
 *   <li>Never logging sensitive payment data</li>
 *   <li>Using Stripe's tokenization for card data</li>
 *   <li>Validating webhook signatures</li>
 *   <li>Following Stripe's security best practices</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Component
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Creates a payment intent in Stripe.
     *
     * @throws PaymentFailedException if the payment intent creation fails
     */
    @Override
    public PaymentIntentResult createPaymentIntent(
            BigDecimal amount,
            String currency,
            String customerId,
            String paymentMethodId,
            String description) {

        try {
            log.info("Creating payment intent for customer: {} with amount: {} {}",
                customerId, amount, currency);

            // Convert amount to cents (Stripe expects smallest currency unit)
            Long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                .setConfirm(false) // Don't auto-confirm, let client confirm
                .addPaymentMethodType("card");

            if (customerId != null) {
                paramsBuilder.setCustomer(customerId);
            }

            if (paymentMethodId != null) {
                paramsBuilder.setPaymentMethod(paymentMethodId);
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            log.info("Payment intent created successfully: {}", paymentIntent.getId());

            return new PaymentIntentResult(
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                amount,
                currency,
                paymentIntent.getClientSecret(),
                paymentIntent.getLatestCharge() != null ? paymentIntent.getLatestCharge() : null
            );

        } catch (StripeException e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Confirms a payment intent in Stripe.
     *
     * @throws PaymentFailedException if the payment intent confirmation fails
     */
    @Override
    public PaymentIntentResult confirmPaymentIntent(String paymentIntentId) {
        try {
            log.info("Confirming payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent confirmedIntent = paymentIntent.confirm();

            log.info("Payment intent confirmed successfully: {}", confirmedIntent.getId());

            return new PaymentIntentResult(
                confirmedIntent.getId(),
                confirmedIntent.getStatus(),
                BigDecimal.valueOf(confirmedIntent.getAmount()).divide(BigDecimal.valueOf(100)),
                confirmedIntent.getCurrency(),
                confirmedIntent.getClientSecret(),
                confirmedIntent.getLatestCharge() != null ? confirmedIntent.getLatestCharge() : null
            );

        } catch (StripeException e) {
            log.error("Failed to confirm payment intent: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to confirm payment intent: " + e.getMessage());
        }
    }

    /**
     * Cancels a payment intent in Stripe.
     *
     * @throws PaymentFailedException if the payment intent cancellation fails
     */
    @Override
    public PaymentIntentResult cancelPaymentIntent(String paymentIntentId) {
        try {
            log.info("Canceling payment intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent canceledIntent = paymentIntent.cancel();

            log.info("Payment intent canceled successfully: {}", canceledIntent.getId());

            return new PaymentIntentResult(
                canceledIntent.getId(),
                canceledIntent.getStatus(),
                BigDecimal.valueOf(canceledIntent.getAmount()).divide(BigDecimal.valueOf(100)),
                canceledIntent.getCurrency(),
                canceledIntent.getClientSecret(),
                null
            );

        } catch (StripeException e) {
            log.error("Failed to cancel payment intent: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to cancel payment intent: " + e.getMessage());
        }
    }

    /**
     * Creates a customer in Stripe.
     *
     * @throws PaymentFailedException if customer creation fails
     */
    @Override
    public CustomerResult createCustomer(String email, String name, String metadata) {
        try {
            log.info("Creating Stripe customer with email: {}", email);

            CustomerCreateParams.Builder paramsBuilder = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name);

            // Parse metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("user_metadata", metadata);
                paramsBuilder.setMetadata(metadataMap);
            }

            Customer customer = Customer.create(paramsBuilder.build());

            log.info("Stripe customer created successfully: {}", customer.getId());

            return new CustomerResult(
                customer.getId(),
                customer.getEmail(),
                customer.getName()
            );

        } catch (StripeException e) {
            log.error("Failed to create Stripe customer: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to create customer: " + e.getMessage());
        }
    }

    /**
     * Updates a customer in Stripe.
     *
     * @throws PaymentFailedException if customer update fails
     */
    @Override
    public CustomerResult updateCustomer(String customerId, String email, String name) {
        try {
            log.info("Updating Stripe customer: {}", customerId);

            Customer customer = Customer.retrieve(customerId);

            CustomerUpdateParams.Builder paramsBuilder = CustomerUpdateParams.builder();

            if (email != null) {
                paramsBuilder.setEmail(email);
            }

            if (name != null) {
                paramsBuilder.setName(name);
            }

            Customer updatedCustomer = customer.update(paramsBuilder.build());

            log.info("Stripe customer updated successfully: {}", updatedCustomer.getId());

            return new CustomerResult(
                updatedCustomer.getId(),
                updatedCustomer.getEmail(),
                updatedCustomer.getName()
            );

        } catch (StripeException e) {
            log.error("Failed to update Stripe customer: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to update customer: " + e.getMessage());
        }
    }

    /**
     * Retrieves a customer from Stripe.
     *
     * @throws PaymentFailedException if customer retrieval fails
     */
    @Override
    public CustomerResult getCustomer(String customerId) {
        try {
            log.info("Retrieving Stripe customer: {}", customerId);

            Customer customer = Customer.retrieve(customerId);

            return new CustomerResult(
                customer.getId(),
                customer.getEmail(),
                customer.getName()
            );

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe customer: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to retrieve customer: " + e.getMessage());
        }
    }

    /**
     * Deletes a customer from Stripe.
     *
     * @throws PaymentFailedException if customer deletion fails
     */
    @Override
    public void deleteCustomer(String customerId) {
        try {
            log.info("Deleting Stripe customer: {}", customerId);

            Customer customer = Customer.retrieve(customerId);
            customer.delete();

            log.info("Stripe customer deleted successfully: {}", customerId);

        } catch (StripeException e) {
            log.error("Failed to delete Stripe customer: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to delete customer: " + e.getMessage());
        }
    }

    /**
     * Attaches a payment method to a customer in Stripe.
     *
     * @throws PaymentFailedException if payment method attachment fails
     */
    @Override
    public PaymentMethodResult attachPaymentMethod(String paymentMethodId, String customerId) {
        try {
            log.info("Attaching payment method {} to customer {}", paymentMethodId, customerId);

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build();

            PaymentMethod attachedMethod = paymentMethod.attach(params);

            log.info("Payment method attached successfully: {}", attachedMethod.getId());

            return mapPaymentMethodResult(attachedMethod);

        } catch (StripeException e) {
            log.error("Failed to attach payment method: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to attach payment method: " + e.getMessage());
        }
    }

    /**
     * Detaches a payment method from a customer in Stripe.
     *
     * @throws PaymentFailedException if payment method detachment fails
     */
    @Override
    public PaymentMethodResult detachPaymentMethod(String paymentMethodId) {
        try {
            log.info("Detaching payment method: {}", paymentMethodId);

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            PaymentMethod detachedMethod = paymentMethod.detach();

            log.info("Payment method detached successfully: {}", detachedMethod.getId());

            return mapPaymentMethodResult(detachedMethod);

        } catch (StripeException e) {
            log.error("Failed to detach payment method: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to detach payment method: " + e.getMessage());
        }
    }

    /**
     * Retrieves a payment method from Stripe.
     *
     * @throws PaymentFailedException if payment method retrieval fails
     */
    @Override
    public PaymentMethodResult getPaymentMethod(String paymentMethodId) {
        try {
            log.info("Retrieving payment method: {}", paymentMethodId);

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

            return mapPaymentMethodResult(paymentMethod);

        } catch (StripeException e) {
            log.error("Failed to retrieve payment method: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to retrieve payment method: " + e.getMessage());
        }
    }

    /**
     * Creates a subscription in Stripe.
     *
     * @throws PaymentFailedException if subscription creation fails
     */
    @Override
    public SubscriptionResult createSubscription(String customerId, String priceId, String paymentMethodId) {
        try {
            log.info("Creating Stripe subscription for customer: {} with price: {}", customerId, priceId);

            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build()
                );

            if (paymentMethodId != null) {
                paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
            }

            Subscription subscription = Subscription.create(paramsBuilder.build());

            log.info("Stripe subscription created successfully: {}", subscription.getId());

            return mapSubscriptionResult(subscription);

        } catch (StripeException e) {
            log.error("Failed to create Stripe subscription: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to create subscription: " + e.getMessage());
        }
    }

    /**
     * Updates a subscription in Stripe.
     *
     * @throws PaymentFailedException if subscription update fails
     */
    @Override
    public SubscriptionResult updateSubscription(String subscriptionId, String newPriceId) {
        try {
            log.info("Updating Stripe subscription {} to price: {}", subscriptionId, newPriceId);

            Subscription subscription = Subscription.retrieve(subscriptionId);

            // Get current subscription item ID
            String subscriptionItemId = subscription.getItems().getData().get(0).getId();

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(subscriptionItemId)
                        .setPrice(newPriceId)
                        .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                .build();

            Subscription updatedSubscription = subscription.update(params);

            log.info("Stripe subscription updated successfully: {}", updatedSubscription.getId());

            return mapSubscriptionResult(updatedSubscription);

        } catch (StripeException e) {
            log.error("Failed to update Stripe subscription: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to update subscription: " + e.getMessage());
        }
    }

    /**
     * Cancels a subscription in Stripe.
     *
     * @throws PaymentFailedException if subscription cancellation fails
     */
    @Override
    public SubscriptionResult cancelSubscription(String subscriptionId, boolean immediately) {
        try {
            log.info("Canceling Stripe subscription {}, immediately: {}", subscriptionId, immediately);

            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (immediately) {
                // Cancel immediately
                Subscription canceledSubscription = subscription.cancel();
                log.info("Stripe subscription canceled immediately: {}", canceledSubscription.getId());
                return mapSubscriptionResult(canceledSubscription);
            } else {
                // Cancel at period end
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();

                Subscription updatedSubscription = subscription.update(params);
                log.info("Stripe subscription set to cancel at period end: {}", updatedSubscription.getId());
                return mapSubscriptionResult(updatedSubscription);
            }

        } catch (StripeException e) {
            log.error("Failed to cancel Stripe subscription: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to cancel subscription: " + e.getMessage());
        }
    }

    /**
     * Retrieves a subscription from Stripe.
     *
     * @throws PaymentFailedException if subscription retrieval fails
     */
    @Override
    public SubscriptionResult getSubscription(String subscriptionId) {
        try {
            log.info("Retrieving Stripe subscription: {}", subscriptionId);

            Subscription subscription = Subscription.retrieve(subscriptionId);

            return mapSubscriptionResult(subscription);

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe subscription: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to retrieve subscription: " + e.getMessage());
        }
    }

    /**
     * Processes a refund for a charge in Stripe.
     *
     * @throws PaymentFailedException if refund processing fails
     */
    @Override
    public RefundResult refundCharge(String chargeId, BigDecimal amount, String reason) {
        try {
            log.info("Processing refund for charge: {} with amount: {}", chargeId, amount);

            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setCharge(chargeId);

            if (amount != null) {
                Long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                paramsBuilder.setAmount(amountInCents);
            }

            if (reason != null) {
                paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
            }

            Refund refund = Refund.create(paramsBuilder.build());

            log.info("Refund processed successfully: {}", refund.getId());

            return new RefundResult(
                refund.getId(),
                refund.getStatus(),
                BigDecimal.valueOf(refund.getAmount()).divide(BigDecimal.valueOf(100)),
                refund.getCurrency()
            );

        } catch (StripeException e) {
            log.error("Failed to process refund: {}", e.getMessage(), e);
            throw PaymentFailedException.withReason("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Validates a Stripe webhook signature.
     *
     * @return true if signature is valid, false otherwise
     */
    @Override
    public boolean validateWebhookSignature(String payload, String signature, String secret) {
        try {
            log.debug("Validating webhook signature");

            // Use the provided secret or fall back to configured webhook secret
            String secretToUse = secret != null ? secret : webhookSecret;

            // Stripe.constructEvent will throw SignatureVerificationException if invalid
            com.stripe.net.Webhook.constructEvent(payload, signature, secretToUse);

            log.debug("Webhook signature validated successfully");
            return true;

        } catch (Exception e) {
            log.warn("Webhook signature validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Maps a Stripe PaymentMethod to our PaymentMethodResult.
     */
    private PaymentMethodResult mapPaymentMethodResult(PaymentMethod paymentMethod) {
        String type = paymentMethod.getType();
        String cardBrand = null;
        String cardLastFour = null;
        Integer cardExpMonth = null;
        Integer cardExpYear = null;

        if ("card".equals(type) && paymentMethod.getCard() != null) {
            PaymentMethod.Card card = paymentMethod.getCard();
            cardBrand = card.getBrand();
            cardLastFour = card.getLast4();
            cardExpMonth = Math.toIntExact(card.getExpMonth());
            cardExpYear = Math.toIntExact(card.getExpYear());
        }

        return new PaymentMethodResult(
            paymentMethod.getId(),
            type,
            cardBrand,
            cardLastFour,
            cardExpMonth,
            cardExpYear
        );
    }

    /**
     * Maps a Stripe Subscription to our SubscriptionResult.
     */
    private SubscriptionResult mapSubscriptionResult(Subscription subscription) {
        LocalDateTime periodStart = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(subscription.getCurrentPeriodStart()),
            ZoneId.systemDefault()
        );

        LocalDateTime periodEnd = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
            ZoneId.systemDefault()
        );

        String priceId = subscription.getItems() != null &&
                        !subscription.getItems().getData().isEmpty()
            ? subscription.getItems().getData().get(0).getPrice().getId()
            : null;

        return new SubscriptionResult(
            subscription.getId(),
            subscription.getStatus(),
            subscription.getCustomer(),
            priceId,
            periodStart,
            periodEnd
        );
    }
}
