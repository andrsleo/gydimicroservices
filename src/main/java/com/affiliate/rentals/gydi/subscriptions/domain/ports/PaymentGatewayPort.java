package com.affiliate.rentals.gydi.subscriptions.domain.ports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Port for payment gateway operations.
 *
 * <p>This interface defines the contract for interacting with external payment gateways
 * (e.g., Stripe) following the hexagonal architecture pattern.</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Never log or store raw payment card data</li>
 *   <li>Always use tokenization provided by the gateway</li>
 *   <li>Validate webhook signatures to prevent fraud</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public interface PaymentGatewayPort {

    /**
     * Creates a payment intent for a subscription payment.
     *
     * @param amount the payment amount
     * @param currency the currency code (e.g., "USD")
     * @param customerId the customer ID in the payment gateway
     * @param paymentMethodId the payment method ID
     * @param description a description of the payment
     * @return the payment intent result containing client secret and intent ID
     */
    PaymentIntentResult createPaymentIntent(
        BigDecimal amount,
        String currency,
        String customerId,
        String paymentMethodId,
        String description
    );

    /**
     * Confirms a payment intent.
     *
     * @param paymentIntentId the payment intent ID
     * @return the updated payment intent result
     */
    PaymentIntentResult confirmPaymentIntent(String paymentIntentId);

    /**
     * Cancels a payment intent.
     *
     * @param paymentIntentId the payment intent ID
     * @return the canceled payment intent result
     */
    PaymentIntentResult cancelPaymentIntent(String paymentIntentId);

    /**
     * Creates a customer in the payment gateway.
     *
     * @param email the customer's email
     * @param name the customer's name
     * @param metadata additional metadata to store with the customer
     * @return the customer result containing the customer ID
     */
    CustomerResult createCustomer(String email, String name, String metadata);

    /**
     * Updates a customer in the payment gateway.
     *
     * @param customerId the customer ID
     * @param email the updated email
     * @param name the updated name
     * @return the updated customer result
     */
    CustomerResult updateCustomer(String customerId, String email, String name);

    /**
     * Retrieves a customer from the payment gateway.
     *
     * @param customerId the customer ID
     * @return the customer result
     */
    CustomerResult getCustomer(String customerId);

    /**
     * Deletes a customer from the payment gateway.
     *
     * @param customerId the customer ID
     */
    void deleteCustomer(String customerId);

    /**
     * Attaches a payment method to a customer.
     *
     * @param paymentMethodId the payment method ID
     * @param customerId the customer ID
     * @return the payment method result
     */
    PaymentMethodResult attachPaymentMethod(String paymentMethodId, String customerId);

    /**
     * Detaches a payment method from a customer.
     *
     * @param paymentMethodId the payment method ID
     * @return the payment method result
     */
    PaymentMethodResult detachPaymentMethod(String paymentMethodId);

    /**
     * Retrieves a payment method.
     *
     * @param paymentMethodId the payment method ID
     * @return the payment method result
     */
    PaymentMethodResult getPaymentMethod(String paymentMethodId);

    /**
     * Creates a subscription in the payment gateway.
     *
     * @param customerId the customer ID
     * @param priceId the price ID (or plan ID)
     * @param paymentMethodId the payment method ID
     * @return the subscription result
     */
    SubscriptionResult createSubscription(String customerId, String priceId, String paymentMethodId);

    /**
     * Updates a subscription in the payment gateway.
     *
     * @param subscriptionId the subscription ID
     * @param newPriceId the new price ID
     * @return the updated subscription result
     */
    SubscriptionResult updateSubscription(String subscriptionId, String newPriceId);

    /**
     * Cancels a subscription in the payment gateway.
     *
     * @param subscriptionId the subscription ID
     * @param immediately if true, cancels immediately; otherwise at period end
     * @return the canceled subscription result
     */
    SubscriptionResult cancelSubscription(String subscriptionId, boolean immediately);

    /**
     * Retrieves a subscription from the payment gateway.
     *
     * @param subscriptionId the subscription ID
     * @return the subscription result
     */
    SubscriptionResult getSubscription(String subscriptionId);

    /**
     * Processes a refund for a charge.
     *
     * @param chargeId the charge ID to refund
     * @param amount the amount to refund (null for full refund)
     * @param reason the refund reason
     * @return the refund result
     */
    RefundResult refundCharge(String chargeId, BigDecimal amount, String reason);

    /**
     * Validates a webhook signature to ensure the event came from the payment gateway.
     *
     * @param payload the raw webhook payload
     * @param signature the signature header
     * @param secret the webhook secret
     * @return true if signature is valid, false otherwise
     */
    boolean validateWebhookSignature(String payload, String signature, String secret);

    /**
     * Result object for payment intent operations.
     */
    record PaymentIntentResult(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String clientSecret,
        String chargeId
    ) {}

    /**
     * Result object for customer operations.
     */
    record CustomerResult(
        String id,
        String email,
        String name
    ) {}

    /**
     * Result object for payment method operations.
     */
    record PaymentMethodResult(
        String id,
        String type,
        String cardBrand,
        String cardLastFour,
        Integer cardExpMonth,
        Integer cardExpYear
    ) {}

    /**
     * Result object for subscription operations.
     */
    record SubscriptionResult(
        String id,
        String status,
        String customerId,
        String priceId,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd
    ) {}

    /**
     * Result object for refund operations.
     */
    record RefundResult(
        String id,
        String status,
        BigDecimal amount,
        String currency
    ) {}
}
