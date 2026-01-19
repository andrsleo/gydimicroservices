package com.affiliate.rentals.gydi.subscriptions.application.service;

import com.affiliate.rentals.gydi.subscriptions.domain.model.*;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentGatewayPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.SubscriptionTransactionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import com.stripe.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for processing Stripe webhook events.
 *
 * <p>This service handles asynchronous events from Stripe and updates
 * the subscription system accordingly. Events include:</p>
 * <ul>
 *   <li>Payment intents (succeeded, failed, canceled)</li>
 *   <li>Subscriptions (created, updated, deleted)</li>
 *   <li>Invoices (payment succeeded, payment failed)</li>
 *   <li>Payment methods (attached, detached)</li>
 * </ul>
 *
 * <p><b>Security:</b> All webhook events MUST be validated using
 * {@link PaymentGatewayPort#validateWebhookSignature(String, String, String)}
 * before processing to prevent fraudulent requests.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final SubscriptionTransactionRepositoryPort transactionRepository;
    private final PaymentGatewayPort paymentGateway;

    public StripeWebhookService(
            UserSubscriptionRepositoryPort subscriptionRepository,
            SubscriptionTransactionRepositoryPort transactionRepository,
            PaymentGatewayPort paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Validates webhook signature.
     *
     * @param payload the raw webhook payload
     * @param signature the Stripe-Signature header
     * @param secret the webhook secret
     * @return true if signature is valid
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        return paymentGateway.validateWebhookSignature(payload, signature, secret);
    }

    /**
     * Processes a Stripe webhook event.
     *
     * <p>This method routes events to the appropriate handler based on event type.</p>
     *
     * @param event the Stripe event to process
     */
    @Transactional
    public void processWebhookEvent(Event event) {
        String eventType = event.getType();

        log.info("Processing Stripe webhook event: {} (ID: {})", eventType, event.getId());

        try {
            switch (eventType) {
                // Payment Intent Events
                case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
                case "payment_intent.canceled" -> handlePaymentIntentCanceled(event);

                // Subscription Events
                case "customer.subscription.created" -> handleSubscriptionCreated(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);

                // Invoice Events
                case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);

                // Payment Method Events
                case "payment_method.attached" -> handlePaymentMethodAttached(event);
                case "payment_method.detached" -> handlePaymentMethodDetached(event);

                default -> log.info("Unhandled webhook event type: {}", eventType);
            }

            log.info("Successfully processed webhook event: {}", eventType);

        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", eventType, e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    // ========== Payment Intent Event Handlers ==========

    /**
     * Handles payment_intent.succeeded event.
     *
     * <p>This event indicates that a payment was successfully processed.</p>
     */
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (paymentIntent == null) {
            log.warn("Payment intent data is null for event: {}", event.getId());
            return;
        }

        log.info("Payment intent succeeded: {} for amount: {}",
            paymentIntent.getId(),
            paymentIntent.getAmount());

        // Extract metadata to find associated subscription
        String subscriptionId = paymentIntent.getMetadata() != null
            ? paymentIntent.getMetadata().get("subscription_id")
            : null;

        if (subscriptionId != null) {
            // Find and update subscription transaction
            // This would typically be implemented with a method to find pending transaction
            log.info("Marking transaction as completed for subscription: {}", subscriptionId);
            // TODO: Update transaction status to COMPLETED
        }
    }

    /**
     * Handles payment_intent.payment_failed event.
     *
     * <p>This event indicates that a payment attempt failed.</p>
     */
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (paymentIntent == null) {
            log.warn("Payment intent data is null for event: {}", event.getId());
            return;
        }

        log.warn("Payment intent failed: {} - Reason: {}",
            paymentIntent.getId(),
            paymentIntent.getLastPaymentError() != null
                ? paymentIntent.getLastPaymentError().getMessage()
                : "Unknown");

        String subscriptionId = paymentIntent.getMetadata() != null
            ? paymentIntent.getMetadata().get("subscription_id")
            : null;

        if (subscriptionId != null) {
            log.warn("Payment failed for subscription: {}", subscriptionId);
            // TODO: Update transaction status to FAILED
            // TODO: Potentially suspend subscription after multiple failures
        }
    }

    /**
     * Handles payment_intent.canceled event.
     */
    private void handlePaymentIntentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (paymentIntent == null) {
            log.warn("Payment intent data is null for event: {}", event.getId());
            return;
        }

        log.info("Payment intent canceled: {}", paymentIntent.getId());

        String subscriptionId = paymentIntent.getMetadata() != null
            ? paymentIntent.getMetadata().get("subscription_id")
            : null;

        if (subscriptionId != null) {
            log.info("Payment canceled for subscription: {}", subscriptionId);
            // TODO: Update transaction status to CANCELED
        }
    }

    // ========== Subscription Event Handlers ==========

    /**
     * Handles customer.subscription.created event.
     *
     * <p>This event is triggered when a subscription is created in Stripe.</p>
     */
    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (subscription == null) {
            log.warn("Subscription data is null for event: {}", event.getId());
            return;
        }

        log.info("Subscription created in Stripe: {} for customer: {}",
            subscription.getId(),
            subscription.getCustomer());

        // Subscription should already exist in our DB (created by SubscribeToPlanUseCase)
        // This webhook confirms it was successfully created in Stripe
        // Update our subscription with the Stripe subscription ID if needed
    }

    /**
     * Handles customer.subscription.updated event.
     *
     * <p>This event is triggered when a subscription is updated (e.g., plan change).</p>
     */
    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (subscription == null) {
            log.warn("Subscription data is null for event: {}", event.getId());
            return;
        }

        log.info("Subscription updated in Stripe: {} - Status: {}",
            subscription.getId(),
            subscription.getStatus());

        String stripeSubscriptionId = subscription.getId();

        // Find subscription in our database by Stripe subscription ID
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            .ifPresent(userSubscription -> {
                // Check if subscription was canceled at period end
                if (subscription.getCancelAtPeriodEnd()) {
                    log.info("Subscription {} set to cancel at period end", stripeSubscriptionId);

                    UserSubscription updated = UserSubscription.builder()
                        .from(userSubscription)
                        .autoRenew(false)
                        .canceledAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                    subscriptionRepository.save(updated);
                }

                // Update subscription status based on Stripe status
                SubscriptionStatus newStatus = mapStripeStatus(subscription.getStatus());
                if (newStatus != userSubscription.status()) {
                    log.info("Updating subscription status from {} to {}",
                        userSubscription.status(), newStatus);

                    UserSubscription updated = UserSubscription.builder()
                        .from(userSubscription)
                        .status(newStatus)
                        .updatedAt(LocalDateTime.now())
                        .build();

                    subscriptionRepository.save(updated);
                }
            });
    }

    /**
     * Handles customer.subscription.deleted event.
     *
     * <p>This event is triggered when a subscription is canceled in Stripe.</p>
     */
    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (subscription == null) {
            log.warn("Subscription data is null for event: {}", event.getId());
            return;
        }

        log.info("Subscription deleted in Stripe: {}", subscription.getId());

        String stripeSubscriptionId = subscription.getId();

        // Find and cancel subscription in our database
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            .ifPresent(userSubscription -> {
                if (userSubscription.status() != SubscriptionStatus.CANCELED) {
                    log.info("Marking subscription as canceled: {}", userSubscription.id());

                    UserSubscription canceled = userSubscription.withCancelation(
                        "Canceled via Stripe webhook"
                    );

                    subscriptionRepository.save(canceled);

                    // Create cancellation transaction
                    LocalDateTime cancelTime = LocalDateTime.now();
                    SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                        .userSubscriptionId(canceled.id())
                        .userId(userSubscription.userId())
                        .paymentMethodId(null) // No payment for cancellations
                        .transactionType(TransactionType.CANCELLATION)
                        .transactionStatus(TransactionStatus.COMPLETED)
                        .fromPlanId(userSubscription.planId())
                        .amount(BigDecimal.ZERO)
                        .currency("USD")
                        .periodStart(cancelTime)
                        .periodEnd(userSubscription.expiresAt() != null ? userSubscription.expiresAt() : cancelTime)
                        .processedAt(cancelTime)
                        .build();

                    transactionRepository.save(transaction);
                }
            });
    }

    // ========== Invoice Event Handlers ==========

    /**
     * Handles invoice.payment_succeeded event.
     *
     * <p>This event is triggered when an invoice payment succeeds (e.g., subscription renewal).</p>
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (invoice == null) {
            log.warn("Invoice data is null for event: {}", event.getId());
            return;
        }

        log.info("Invoice payment succeeded: {} for amount: {}",
            invoice.getId(),
            invoice.getAmountPaid());

        String stripeSubscriptionId = invoice.getSubscription();

        if (stripeSubscriptionId != null) {
            subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(userSubscription -> {
                    log.info("Recording renewal payment for subscription: {}", userSubscription.id());

                    // Create renewal transaction
                    LocalDateTime periodStart = invoice.getPeriodStart() != null
                        ? LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(invoice.getPeriodStart()),
                            java.time.ZoneId.systemDefault())
                        : LocalDateTime.now();

                    LocalDateTime periodEnd = invoice.getPeriodEnd() != null
                        ? LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(invoice.getPeriodEnd()),
                            java.time.ZoneId.systemDefault())
                        : null;

                    SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                        .userSubscriptionId(userSubscription.id())
                        .userId(userSubscription.userId())
                        .paymentMethodId(userSubscription.paymentMethodId())
                        .transactionType(TransactionType.RENEWAL)
                        .transactionStatus(TransactionStatus.COMPLETED)
                        .toPlanId(userSubscription.planId())
                        .amount(BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100)))
                        .currency(invoice.getCurrency())
                        .stripeInvoiceId(invoice.getId())
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .processedAt(LocalDateTime.now())
                        .build();

                    transactionRepository.save(transaction);

                    // Update next billing date
                    if (invoice.getPeriodEnd() != null) {
                        LocalDateTime nextBillingDate = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(invoice.getPeriodEnd()),
                            java.time.ZoneId.systemDefault()
                        );

                        UserSubscription updated = UserSubscription.builder()
                            .from(userSubscription)
                            .nextBillingDate(nextBillingDate)
                            .expiresAt(nextBillingDate)
                            .updatedAt(LocalDateTime.now())
                            .build();

                        subscriptionRepository.save(updated);
                    }
                });
        }
    }

    /**
     * Handles invoice.payment_failed event.
     *
     * <p>This event is triggered when an invoice payment fails (e.g., expired card).</p>
     */
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (invoice == null) {
            log.warn("Invoice data is null for event: {}", event.getId());
            return;
        }

        log.warn("Invoice payment failed: {} for amount: {}",
            invoice.getId(),
            invoice.getAmountDue());

        String stripeSubscriptionId = invoice.getSubscription();

        if (stripeSubscriptionId != null) {
            subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(userSubscription -> {
                    log.warn("Payment failed for subscription: {}", userSubscription.id());

                    // Create failed transaction
                    LocalDateTime periodStart = invoice.getPeriodStart() != null
                        ? LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(invoice.getPeriodStart()),
                            java.time.ZoneId.systemDefault())
                        : LocalDateTime.now();

                    LocalDateTime periodEnd = invoice.getPeriodEnd() != null
                        ? LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(invoice.getPeriodEnd()),
                            java.time.ZoneId.systemDefault())
                        : null;

                    SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                        .userSubscriptionId(userSubscription.id())
                        .userId(userSubscription.userId())
                        .paymentMethodId(userSubscription.paymentMethodId())
                        .transactionType(TransactionType.RENEWAL)
                        .transactionStatus(TransactionStatus.FAILED)
                        .toPlanId(userSubscription.planId())
                        .amount(BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100)))
                        .currency(invoice.getCurrency())
                        .stripeInvoiceId(invoice.getId())
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .processedAt(LocalDateTime.now())
                        .build();

                    transactionRepository.save(transaction);

                    // After multiple failures, Stripe will cancel the subscription
                    // This will trigger customer.subscription.deleted webhook
                });
        }
    }

    // ========== Payment Method Event Handlers ==========

    /**
     * Handles payment_method.attached event.
     */
    private void handlePaymentMethodAttached(Event event) {
        com.stripe.model.PaymentMethod paymentMethod = (com.stripe.model.PaymentMethod) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (paymentMethod == null) {
            log.warn("Payment method data is null for event: {}", event.getId());
            return;
        }

        log.info("Payment method attached: {} to customer: {}",
            paymentMethod.getId(),
            paymentMethod.getCustomer());

        // Payment method should already exist in our DB
        // This webhook confirms it was successfully attached
    }

    /**
     * Handles payment_method.detached event.
     */
    private void handlePaymentMethodDetached(Event event) {
        com.stripe.model.PaymentMethod paymentMethod = (com.stripe.model.PaymentMethod) event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);

        if (paymentMethod == null) {
            log.warn("Payment method data is null for event: {}", event.getId());
            return;
        }

        log.info("Payment method detached: {}", paymentMethod.getId());

        // Mark payment method as inactive in our database
        // This would be implemented with a method to find by gatewayToken
        // TODO: Mark payment method as inactive
    }

    // ========== Helper Methods ==========

    /**
     * Maps Stripe subscription status to our SubscriptionStatus enum.
     */
    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid" -> SubscriptionStatus.SUSPENDED;
            case "canceled", "incomplete_expired" -> SubscriptionStatus.CANCELED;
            default -> {
                log.warn("Unknown Stripe status: {}, defaulting to ACTIVE", stripeStatus);
                yield SubscriptionStatus.ACTIVE;
            }
        };
    }
}
