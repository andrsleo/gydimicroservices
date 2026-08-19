package com.affiliate.rentals.gydi.subscriptions.infrastructure.scheduler;

import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to automatically downgrade expired subscriptions to FREE plan.
 * <p>
 * Runs daily to check for subscriptions that have expired and downgrades them
 * from paid plans (PRO, ELITE) to the FREE plan.
 * </p>
 * <p>
 * This handles the scenario where a user cancels at end-of-period:
 * - User keeps their paid plan until expiresAt
 * - This scheduler detects expiration and downgrades to FREE
 * </p>
 */
@Component
public class ExpiredSubscriptionDowngradeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSubscriptionDowngradeScheduler.class);

    private final UserSubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;

    public ExpiredSubscriptionDowngradeScheduler(
            UserSubscriptionRepositoryPort subscriptionRepository,
            PlanRepositoryPort planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    /**
     * Runs daily at 1:00 AM to downgrade expired subscriptions.
     * <p>
     * Cron expression: 0 0 1 * * * (At 01:00 AM every day)
     * For testing use: 0 *&#47;5 * * * * (Every 5 minutes)
     * </p>
     */
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    @Transactional
    public void downgradeExpiredSubscriptions() {
        log.info("Starting scheduled downgrade of expired subscriptions...");
        LocalDateTime now = LocalDateTime.now();

        // Find FREE plan (target for downgrade)
        Plan freePlan = planRepository.findByPlanCode("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE plan not found in database"));

        // Find all subscriptions that have expired and are not already FREE
        List<UserSubscription> expiredSubscriptions = findExpiredNonFreeSubscriptions(now, freePlan.id());

        log.info("Found {} expired subscriptions to downgrade to FREE", expiredSubscriptions.size());

        int downgraded = 0;
        int errors = 0;

        for (UserSubscription subscription : expiredSubscriptions) {
            try {
                downgradeToFree(subscription, freePlan, now);
                downgraded++;
            } catch (Exception e) {
                errors++;
                log.error("Failed to downgrade subscription {} for user {}: {}",
                        subscription.id(), subscription.userId(), e.getMessage(), e);
            }
        }

        log.info("Completed downgrade process. Downgraded: {}, Errors: {}", downgraded, errors);
    }

    /**
     * Finds all subscriptions that are expired and not on FREE plan.
     */
    private List<UserSubscription> findExpiredNonFreeSubscriptions(LocalDateTime now, Long freePlanId) {
        // Use findExpiringBefore to get subscriptions that expired
        return subscriptionRepository.findExpiringBefore(now).stream()
                .filter(sub -> !sub.planId().equals(freePlanId)) // Not already FREE
                .filter(sub -> sub.status() == SubscriptionStatus.ACTIVE
                            || sub.status() == SubscriptionStatus.CANCELED) // Active or canceled
                .toList();
    }

    /**
     * Downgrades a subscription to FREE plan.
     */
    private void downgradeToFree(UserSubscription subscription, Plan freePlan, LocalDateTime now) {
        log.info("Downgrading subscription {} (user: {}) from plan {} to FREE",
                subscription.id(), subscription.userId(), subscription.planId());

        // Create updated subscription with FREE plan
        UserSubscription downgradedSubscription = UserSubscription.builder()
                .from(subscription)
                .planId(freePlan.id())
                .status(SubscriptionStatus.ACTIVE) // FREE is active
                .expiresAt(null) // FREE doesn't expire
                .autoRenew(false) // FREE doesn't auto-renew
                .stripeSubscriptionId(null) // No Stripe subscription for FREE
                .updatedAt(now)
                .build();

        subscriptionRepository.save(downgradedSubscription);

        log.info("Successfully downgraded user {} to FREE plan", subscription.userId());
    }
}
