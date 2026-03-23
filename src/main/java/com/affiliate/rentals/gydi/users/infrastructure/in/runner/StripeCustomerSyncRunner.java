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
 * Application startup task that creates Stripe Platform Customer records for
 * active users that do not yet have one in their Connect account.
 *
 * <p>Post V95 migration: stripe_customer_id has been removed from users.users.
 * Customer IDs (cus_xxx) are now stored in
 * commissions.stripe_connect_accounts.stripe_platform_customer_id.</p>
 *
 * <h2>Idempotency</h2>
 * <p>The query only returns users whose Connect account has NULL
 * stripe_platform_customer_id. The update is additionally guarded so
 * concurrent runs cannot overwrite a value already set.</p>
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

    @Override
    public void run(ApplicationArguments args) {
        if (!isStripeConfigured()) {
            log.warn("StripeCustomerSyncRunner: STRIPE_API_KEY is not configured — "
                    + "skipping Stripe customer sync. "
                    + "Set the stripe.api-key property to enable this feature.");
            return;
        }

        List<UserStripeInfo> pending = userStripePort.findActiveUsersWithoutPlatformCustomer();

        if (pending.isEmpty()) {
            log.info("StripeCustomerSyncRunner: All active users already have a Stripe Platform Customer ID — nothing to sync.");
            return;
        }

        log.info("StripeCustomerSyncRunner: Found {} active user(s) without a Stripe Platform Customer ID. Starting sync...",
                pending.size());

        int created = 0;
        int failed = 0;
        int total = pending.size();

        for (int batchStart = 0; batchStart < total; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, total);
            List<UserStripeInfo> batch = pending.subList(batchStart, batchEnd);

            log.debug("StripeCustomerSyncRunner: Processing batch [{}/{}]", batchEnd, total);

            for (UserStripeInfo user : batch) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("StripeCustomerSyncRunner: Interrupted during sync — created={}, failed={}, remaining={}",
                            created, failed, total - created - failed);
                    return;
                }

                try {
                    String customerId = createStripeCustomer(user);
                    userStripePort.updatePlatformCustomerId(user.userId(), customerId);
                    created++;
                    log.info("StripeCustomerSyncRunner: Created Stripe Platform Customer {} for user id={} email={}",
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

    private boolean isStripeConfigured() {
        return Stripe.apiKey != null && !Stripe.apiKey.isBlank();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
