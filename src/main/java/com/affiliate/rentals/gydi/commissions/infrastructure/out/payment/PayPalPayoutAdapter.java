package com.affiliate.rentals.gydi.commissions.infrastructure.out.payment;

import com.affiliate.rentals.gydi.commissions.domain.ports.PayoutGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * PayPal Payouts API adapter for affiliate commission payouts.
 * <p>
 * Uses Spring RestClient directly — no PayPal SDK to avoid classpath conflicts.
 * </p>
 * <p>
 * Flow:
 * <ol>
 *   <li>Obtain OAuth2 access token via client_credentials grant (cached until expiry)</li>
 *   <li>Create single-item Batch Payout via POST /v1/payments/payouts</li>
 *   <li>Return PayoutResult with batch ID and item ID</li>
 * </ol>
 * </p>
 * <p>
 * PayPal processes payouts asynchronously. Funds typically arrive within minutes
 * for verified PayPal accounts but may take up to 1 business day.
 * </p>
 *
 * <p>Active only in the {@code prod} profile. For local/dev, {@link DevPayoutAdapter} is used.</p>
 */
@Component
@Profile("prod")
public class PayPalPayoutAdapter implements PayoutGatewayPort {

    private static final Logger logger = LoggerFactory.getLogger(PayPalPayoutAdapter.class);

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    // Token cache — guarded by lock
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private final RestClient restClient;

    public PayPalPayoutAdapter() {
        this.restClient = RestClient.builder().build();
    }

    // ============================================================================
    // PUBLIC API
    // ============================================================================

    @Override
    public PayoutResult sendPayout(
            String recipientPayPalEmail,
            BigDecimal amount,
            String currency,
            String referenceId,
            String note
    ) {
        logger.info("Initiating PayPal payout: recipientEmail={}, amount={} {}, referenceId={}",
                recipientPayPalEmail, amount, currency, referenceId);

        try {
            String accessToken = getAccessToken();
            String senderBatchId = "gydi-" + UUID.randomUUID();

            Map<String, Object> requestBody = buildPayoutRequest(
                    senderBatchId, recipientPayPalEmail, amount, currency, referenceId, note);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/v1/payments/payouts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                logger.error("PayPal Payouts API returned null response for referenceId={}", referenceId);
                return PayoutResult.failure("PayPal returned empty response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> batchHeader = (Map<String, Object>) response.get("batch_header");
            String payoutBatchId = batchHeader != null ? (String) batchHeader.get("payout_batch_id") : null;

            // PayPal immediately returns batch status — items are populated asynchronously
            // We store the sender_batch_id and batch_id for reconciliation
            String batchStatus = batchHeader != null ? (String) batchHeader.get("batch_status") : "PENDING";

            logger.info("PayPal payout accepted: batchId={}, senderBatchId={}, status={}, referenceId={}",
                    payoutBatchId, senderBatchId, batchStatus, referenceId);

            // Return senderBatchId as payoutItemId for now; webhook/polling will update later
            return PayoutResult.success(payoutBatchId, senderBatchId, batchStatus);

        } catch (Exception e) {
            logger.error("PayPal payout failed for referenceId={}: {}", referenceId, e.getMessage(), e);
            return PayoutResult.failure(formatError(e));
        }
    }

    // ============================================================================
    // PRIVATE: TOKEN MANAGEMENT
    // ============================================================================

    /**
     * Returns a valid access token, fetching a new one if the cached one has expired.
     * Thread-safe via ReentrantLock.
     */
    private String getAccessToken() {
        // Fast path: cached token still valid (with 60s safety margin)
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedAccessToken;
        }

        tokenLock.lock();
        try {
            // Re-check after acquiring lock (another thread may have refreshed)
            if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
                return cachedAccessToken;
            }

            logger.debug("Fetching new PayPal access token from {}", baseUrl);

            String credentials = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes());

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(baseUrl + "/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new IllegalStateException("PayPal token response missing access_token");
            }

            cachedAccessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.getOrDefault("expires_in", 32400);
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

            logger.debug("PayPal access token obtained, expires in {}s", expiresIn);
            return cachedAccessToken;

        } finally {
            tokenLock.unlock();
        }
    }

    // ============================================================================
    // PRIVATE: REQUEST BUILDER
    // ============================================================================

    private Map<String, Object> buildPayoutRequest(
            String senderBatchId,
            String recipientEmail,
            BigDecimal amount,
            String currency,
            String referenceId,
            String note
    ) {
        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        Map<String, Object> senderBatchHeader = Map.of(
                "sender_batch_id", senderBatchId,
                "email_subject", "Tu comision GYDI ha sido enviada",
                "email_message", "Has recibido el pago de tu comision de referidos GYDI."
        );

        Map<String, Object> amountObj = Map.of(
                "value", formattedAmount,
                "currency", currency.toUpperCase()
        );

        Map<String, Object> item = Map.of(
                "recipient_type", "EMAIL",
                "amount", amountObj,
                "receiver", recipientEmail,
                "note", note != null ? note : "Comision GYDI",
                "sender_item_id", "gydi-item-" + referenceId
        );

        return Map.of(
                "sender_batch_header", senderBatchHeader,
                "items", List.of(item)
        );
    }

    // ============================================================================
    // PRIVATE: ERROR FORMATTING
    // ============================================================================

    private String formatError(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("RECEIVER_UNREGISTERED")) {
            return "PayPal account not found for this email address";
        }
        if (message != null && message.contains("INSUFFICIENT_FUNDS")) {
            return "Insufficient funds in PayPal sender account";
        }
        if (message != null && message.contains("401")) {
            return "PayPal authentication failed — check client credentials";
        }
        return message != null ? message : "Unknown PayPal error";
    }
}
