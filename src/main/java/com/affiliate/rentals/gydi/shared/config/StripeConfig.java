package com.affiliate.rentals.gydi.shared.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Stripe Payment Gateway integration.
 * Initializes the Stripe SDK with API credentials and sets global
 * configuration.
 *
 * <p>
 * Security considerations:
 * <ul>
 * <li>API keys must be provided via environment variables</li>
 * <li>Never commit production keys to version control</li>
 * <li>Use test keys (sk_test_...) for development</li>
 * <li>Use live keys (sk_live_...) only in production</li>
 * </ul>
 *
 * @see <a href="https://stripe.com/docs/api">Stripe API Documentation</a>
 */
@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.publishable-key}")
    private String publishableKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.currency:USD}")
    private String currency;

    /**
     * Initializes Stripe SDK with API credentials after bean construction.
     * This method is automatically called by Spring after dependency injection.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;

        String keyPrefix = apiKey.substring(0, Math.min(7, apiKey.length()));
        boolean isTestMode = keyPrefix.contains("test");

        log.info("Stripe SDK initialized successfully");
        log.info("Stripe mode: {}", isTestMode ? "TEST" : "LIVE");
        log.info("Stripe currency: {}", currency);
        log.info("Publishable key configured: {}", publishableKey.substring(0, 12) + "...");

        if (!isTestMode) {
            log.warn("⚠️  STRIPE LIVE MODE DETECTED - Processing real payments!");
        }
    }

    /**
     * Returns the Stripe publishable key for client-side usage.
     * This key is safe to expose in frontend applications.
     *
     * @return Stripe publishable key
     */
    public String getPublishableKey() {
        return publishableKey;
    }

    /**
     * Returns the Stripe webhook secret for signature verification.
     * This secret is used to verify that webhook events come from Stripe.
     *
     * @return Stripe webhook signing secret
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Returns the default currency for payments.
     *
     * @return Currency code (e.g., "USD", "EUR")
     */
    public String getCurrency() {
        return currency;
    }
}