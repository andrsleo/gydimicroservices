package com.affiliate.rentals.gydi.users.infrastructure.in.runner;

import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort;
import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort.UserStripeInfo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application startup task that creates Stripe Customer records for all active
 * users that do not yet have one.
 *
 * <p>This runner replaces the Java-based Flyway migration {@code V93} that was
 * previously used for the same purpose.  Pure SQL cannot call the Stripe API,
 * so the actual {@link Customer#create} calls are made here instead.  The
 * companion SQL migration {@code V93__create_stripe_customers.sql} still runs
 * via Flyway to create the partial index and log the pending count; it does NOT
 * attempt to create customers itself.</p>
 *
 * <h2>Idempotency</h2>
 * <p>The port query only returns users where {@code stripe_customer_id IS NULL
 * AND is_active = TRUE}.  The update is additionally guarded with
 * {@code AND stripe_customer_id IS NULL} in the SQL so that concurrent runs or
 * restarts cannot overwrite a value that was already persisted.</p>
 *
 * <h2>Fault tolerance</h2>
 * <ul>
 *   <li>If the Stripe API key is not configured the runner logs a warning and
 *       exits without error — the application starts normally.</li>
 *   <li>If Stripe throws an exception for a single user the error is logged and
 *       processing continues with the next user.  The app never fails to start
 *       because of this runner.</li>
 * </ul>
 *
 * <h2>Rate limiting</h2>
 * <p>Users are processed in batches of {@value #BATCH_SIZE}.  A
 * {@value #DELAY_BETWEEN_REQUESTS_MS} ms pause is inserted between individual
 * requests and a {@value #DELAY_BETWEEN_BATCHES_MS} ms pause between batches to
 * stay within Stripe's default rate limits.</p>
 *
 * @author GYDI Development Team
 * @see UserStripePort
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class StripeCustomerSyncRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 50;
    private static final long DELAY_BETWEEN_REQUESTS_MS = 100L;
    private static final long DELAY_BETWEEN_BATCHES_MS = 500L;

    private final UserStripePort userStripePort;

    // ── ApplicationRunner ─────────────────────────────────────────────────────

    /**
     * Entry point called by Spring Boot once the application context is fully
     * refreshed.
     *
     * @param args application arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!isStripeConfigured()) {
            log.warn("StripeCustomerSyncRunner: STRIPE_API_KEY is not configured — "
                    + "skipping Stripe customer sync. "
                    + "Set the stripe.api-key property to enable this feature.");
            return;
        }

        List<UserStripeInfo> pending = userStripePort.findActiveUsersWithoutStripeCustomer();

        if (pending.isEmpty()) {
            log.info("StripeCustomerSyncRunner: All active users already have a Stripe customer ID — nothing to sync.");
            return;
        }

        log.info("StripeCustomerSyncRunner: Found {} active user(s) without a Stripe customer ID. Starting sync...",
                pending.size());

        int created = 0;
        int failed = 0;
        int total = pending.size();

        for (int batchStart = 0; batchStart < total; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, total);
            List<UserStripeInfo> batch = pending.subList(batchStart, batchEnd);

            log.debug("StripeCustomerSyncRunner: Processing batch [{}/{}]",
                    batchEnd, total);

            for (UserStripeInfo user : batch) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("StripeCustomerSyncRunner: Interrupted during sync — "
                            + "created={}, failed={}, remaining={}",
                            created, failed, total - created - failed);
                    return;
                }

                try {
                    String customerId = createStripeCustomer(user);
                    userStripePort.updateStripeCustomerId(user.userId(), customerId);
                    created++;
                    log.info("StripeCustomerSyncRunner: Created Stripe customer {} for user id={} email={}",
                            customerId, user.userId(), user.email());
                } catch (StripeException e) {
                    failed++;
                    log.error("StripeCustomerSyncRunner: Stripe API error for user id={} email={} — [{}] {}",
                            user.userId(), user.email(), e.getCode(), e.getMessage());
                } catch (Exception e) {
                    failed++;
                    log.error("StripeCustomerSyncRunner: Unexpected error for user id={} email={} — {}",
                            user.userId(), user.email(), e.getMessage(), e);
                }

                sleepQuietly(DELAY_BETWEEN_REQUESTS_MS);
            }

            if (batchEnd < total) {
                sleepQuietly(DELAY_BETWEEN_BATCHES_MS);
            }
        }

        log.info("StripeCustomerSyncRunner: Sync complete — created={}, failed={}, total={}",
                created, failed, total);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Calls the Stripe API to create a Customer and returns its ID.
     *
     * @param user the user to create a Stripe Customer for
     * @return the newly created Stripe Customer ID (e.g. {@code cus_xxxxx})
     * @throws StripeException if the Stripe API call fails
     */
    private String createStripeCustomer(UserStripeInfo user) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.email())
                .putMetadata("gydi_user_id", String.valueOf(user.userId()))
                .putMetadata("plan_code", user.planCode() != null ? user.planCode() : "UNKNOWN")
                .putMetadata("country", user.country() != null ? user.country() : "unknown")
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }

    /**
     * Returns {@code true} when the Stripe SDK has been initialised with a
     * non-blank API key.
     *
     * <p>The key is set by {@link com.affiliate.rentals.gydi.shared.config.StripeConfig}
     * via its {@code @PostConstruct} method.  Checking
     * {@link Stripe#apiKey} directly avoids introducing an extra dependency on
     * the config bean.</p>
     */
    private boolean isStripeConfigured() {
        return Stripe.apiKey != null && !Stripe.apiKey.isBlank();
    }

    /**
     * Sleeps for the given number of milliseconds, logging and re-interrupting
     * the thread if interrupted.
     *
     * @param millis the duration to sleep
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
