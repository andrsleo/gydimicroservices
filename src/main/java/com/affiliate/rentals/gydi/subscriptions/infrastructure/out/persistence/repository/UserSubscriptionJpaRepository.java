package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity.SubscriptionStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for UserSubscriptionEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for user subscriptions.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to subscription management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * UserSubscriptionRepositoryAdapter to implement the UserSubscriptionRepositoryPort.</p>
 *
 * @author GYDI Development Team
 * @see UserSubscriptionEntity
 */
@Repository
public interface UserSubscriptionJpaRepository extends JpaRepository<UserSubscriptionEntity, Long> {

    /**
     * Finds the subscription for a specific user.
     *
     * @param userId the user ID
     * @return an Optional containing the subscription if found, empty otherwise
     */
    Optional<UserSubscriptionEntity> findByUserId(Long userId);

    /**
     * Finds a subscription by Stripe subscription ID.
     *
     * @param stripeSubscriptionId the Stripe subscription ID
     * @return an Optional containing the subscription if found, empty otherwise
     */
    Optional<UserSubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Finds all subscriptions with a specific status.
     *
     * @param status the subscription status
     * @return a list of subscriptions with the given status
     */
    List<UserSubscriptionEntity> findByStatus(SubscriptionStatusEntity status);

    /**
     * Finds all subscriptions for a specific plan.
     *
     * @param planId the plan ID
     * @return a list of subscriptions for the given plan
     */
    List<UserSubscriptionEntity> findByPlanId(Long planId);

    /**
     * Finds all subscriptions that expire before a given date.
     *
     * @param expirationDate the date to check against
     * @return a list of subscriptions expiring before the given date
     */
    @Query("SELECT s FROM UserSubscriptionEntity s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :expirationDate AND s.status = 'ACTIVE'")
    List<UserSubscriptionEntity> findExpiringBefore(@Param("expirationDate") LocalDateTime expirationDate);

    /**
     * Finds all active subscriptions with auto-renewal enabled.
     *
     * @return a list of subscriptions with auto-renewal
     */
    @Query("SELECT s FROM UserSubscriptionEntity s WHERE s.status = 'ACTIVE' AND s.autoRenew = true")
    List<UserSubscriptionEntity> findActiveWithAutoRenewal();

    /**
     * Finds all subscriptions with next billing date before a given date.
     *
     * @param billingDate the date to check against
     * @return a list of subscriptions due for billing
     */
    @Query("SELECT s FROM UserSubscriptionEntity s WHERE s.nextBillingDate IS NOT NULL AND s.nextBillingDate < :billingDate AND s.status = 'ACTIVE'")
    List<UserSubscriptionEntity> findDueForBillingBefore(@Param("billingDate") LocalDateTime billingDate);

    /**
     * Checks if a user has an active subscription.
     *
     * @param userId the user ID
     * @return true if user has an active subscription, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSubscriptionEntity s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    boolean hasActiveSubscription(@Param("userId") Long userId);

    /**
     * Counts subscriptions by plan ID.
     *
     * @param planId the plan ID
     * @return the number of subscriptions for the given plan
     */
    long countByPlanId(Long planId);
}
