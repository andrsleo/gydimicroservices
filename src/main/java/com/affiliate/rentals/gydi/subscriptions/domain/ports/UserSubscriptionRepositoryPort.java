package com.affiliate.rentals.gydi.subscriptions.domain.ports;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for UserSubscription aggregate.
 *
 * <p>This interface defines the contract for user subscription persistence operations
 * following the hexagonal architecture pattern.</p>
 *
 * @author GYDI Development Team
 */
public interface UserSubscriptionRepositoryPort {

    /**
     * Persists a new subscription or updates an existing one.
     *
     * @param subscription the subscription to save
     * @return the saved subscription with generated ID if it was new
     */
    UserSubscription save(UserSubscription subscription);

    /**
     * Finds a subscription by its unique identifier.
     *
     * @param id the subscription ID
     * @return an Optional containing the subscription if found, empty otherwise
     */
    Optional<UserSubscription> findById(Long id);

    /**
     * Finds the active subscription for a specific user.
     *
     * @param userId the user ID
     * @return an Optional containing the active subscription if found, empty otherwise
     */
    Optional<UserSubscription> findByUserId(Long userId);

    /**
     * Finds a subscription by Stripe subscription ID.
     *
     * @param stripeSubscriptionId the Stripe subscription ID
     * @return an Optional containing the subscription if found, empty otherwise
     */
    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Retrieves all subscriptions with a specific status.
     *
     * @param status the subscription status
     * @return a list of subscriptions with the given status
     */
    List<UserSubscription> findByStatus(SubscriptionStatus status);

    /**
     * Retrieves all subscriptions for a specific plan.
     *
     * @param planId the plan ID
     * @return a list of subscriptions for the given plan
     */
    List<UserSubscription> findByPlanId(Long planId);

    /**
     * Retrieves all subscriptions that expire before a given date.
     *
     * @param expirationDate the date to check against
     * @return a list of subscriptions expiring before the given date
     */
    List<UserSubscription> findExpiringBefore(LocalDateTime expirationDate);

    /**
     * Retrieves all active subscriptions with auto-renewal enabled.
     *
     * @return a list of subscriptions with auto-renewal
     */
    List<UserSubscription> findActiveWithAutoRenewal();

    /**
     * Retrieves all subscriptions with next billing date before a given date.
     *
     * @param billingDate the date to check against
     * @return a list of subscriptions due for billing
     */
    List<UserSubscription> findDueForBillingBefore(LocalDateTime billingDate);

    /**
     * Checks if a user has an active subscription.
     *
     * @param userId the user ID
     * @return true if user has an active subscription, false otherwise
     */
    boolean hasActiveSubscription(Long userId);

    /**
     * Deletes a subscription by its ID.
     *
     * @param id the ID of the subscription to delete
     */
    void deleteById(Long id);

    /**
     * Counts subscriptions by plan ID.
     *
     * @param planId the plan ID
     * @return the number of subscriptions for the given plan
     */
    long countByPlanId(Long planId);
}
