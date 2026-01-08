package com.affiliate.rentals.gydi.subscriptions.infrastructure.in.rest;

import com.affiliate.rentals.gydi.subscriptions.application.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for receiving Stripe webhook events.
 *
 * <p>This controller handles asynchronous events from Stripe, including:</p>
 * <ul>
 *   <li>Payment intents (succeeded, failed, canceled)</li>
 *   <li>Subscriptions (created, updated, deleted)</li>
 *   <li>Invoices (payment succeeded, payment failed)</li>
 *   <li>Payment methods (attached, detached)</li>
 * </ul>
 *
 * <p><b>Security:</b> This endpoint validates the Stripe webhook signature
 * to ensure events are genuine and haven't been tampered with.</p>
 *
 * <p><b>Configuration:</b> The webhook secret must be configured in application.yml:</p>
 * <pre>
 * stripe:
 *   webhook-secret: whsec_...
 * </pre>
 *
 * <p><b>Stripe Dashboard Setup:</b></p>
 * <ol>
 *   <li>Go to Developers → Webhooks in Stripe Dashboard</li>
 *   <li>Add endpoint: https://yourdomain.com/api/webhooks/stripe</li>
 *   <li>Select events to listen to</li>
 *   <li>Copy the webhook signing secret to application.yml</li>
 * </ol>
 *
 * <p><b>Access:</b> Public endpoint (authenticated via webhook signature)</p>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Stripe Webhooks", description = "Endpoint for receiving Stripe webhook events")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookService webhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Receives and processes Stripe webhook events.
     *
     * <p>This endpoint is called by Stripe when events occur (e.g., payment succeeded,
     * subscription canceled). It validates the signature and processes the event.</p>
     *
     * <p><b>Important:</b> This endpoint must return a 200 status code within 30 seconds
     * or Stripe will consider it failed and retry the webhook.</p>
     *
     * <p><b>Supported Events:</b></p>
     * <ul>
     *   <li>payment_intent.succeeded</li>
     *   <li>payment_intent.payment_failed</li>
     *   <li>payment_intent.canceled</li>
     *   <li>customer.subscription.created</li>
     *   <li>customer.subscription.updated</li>
     *   <li>customer.subscription.deleted</li>
     *   <li>invoice.payment_succeeded</li>
     *   <li>invoice.payment_failed</li>
     *   <li>payment_method.attached</li>
     *   <li>payment_method.detached</li>
     * </ul>
     *
     * @param payload the raw webhook payload from Stripe
     * @param signatureHeader the Stripe-Signature header for validation
     * @return success response or error
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Receive Stripe webhook events",
        description = "This endpoint receives asynchronous events from Stripe. " +
                     "It validates the webhook signature and processes the event. " +
                     "This endpoint is public but authenticated via Stripe's webhook signature."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature or payload"),
        @ApiResponse(responseCode = "500", description = "Internal server error while processing webhook")
    })
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature")
            @Parameter(description = "Stripe webhook signature for validation", required = true)
            String signatureHeader) {

        log.info("Received Stripe webhook event");

        Event event;

        try {
            // Validate webhook signature
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);

            log.info("Webhook signature validated successfully for event: {} (ID: {})",
                event.getType(), event.getId());

        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature: {}", e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid signature");
            errorResponse.put("message", "Webhook signature verification failed");

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
        } catch (Exception e) {
            log.error("Error parsing webhook payload: {}", e.getMessage(), e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid payload");
            errorResponse.put("message", "Failed to parse webhook payload");

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
        }

        try {
            // Process the validated event
            webhookService.processWebhookEvent(event);

            log.info("Webhook event processed successfully: {}", event.getType());

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("eventId", event.getId());
            successResponse.put("eventType", event.getType());

            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", event.getType(), e.getMessage(), e);

            // Return 200 to prevent Stripe from retrying
            // Log the error for manual investigation
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("eventId", event.getId());
            errorResponse.put("eventType", event.getType());
            errorResponse.put("error", e.getMessage());

            // Note: We return 200 even on processing errors to prevent Stripe retries
            // The error is logged and can be investigated manually
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Health check endpoint for webhook monitoring.
     *
     * <p>This endpoint can be used to verify that the webhook endpoint is reachable
     * and the server is running.</p>
     *
     * @return health status
     */
    @GetMapping("/stripe/health")
    @Operation(
        summary = "Webhook health check",
        description = "Verifies that the Stripe webhook endpoint is reachable and operational."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook endpoint is healthy")
    })
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "stripe-webhooks");
        response.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
