package com.affiliate.rentals.gydi.users.domain.ports;

import java.util.List;

/**
 * Port for Stripe-related user persistence operations.
 *
 * <p>Post V95 migration: stripe_customer_id no longer lives in users.users.
 * Platform Customer IDs (cus_xxx) are now stored in
 * commissions.stripe_connect_accounts.stripe_platform_customer_id.</p>
 *
 * <p>This port exposes the two operations required by the
 * {@code StripeCustomerSyncRunner} startup task.</p>
 *
 * @author GYDI Development Team
 * @see com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter.UserStripeAdapter
 * @see com.affiliate.rentals.gydi.users.infrastructure.in.runner.StripeCustomerSyncRunner
 */
public interface UserStripePort {

    /**
     * Returns all active users that do not yet have a Stripe Platform Customer ID
     * in their Connect account record.
     *
     * @return a list of {@link UserStripeInfo} projections; never {@code null}, may be empty
     */
    List<UserStripeInfo> findActiveUsersWithoutPlatformCustomer();

    /**
     * Persists the given Stripe Platform Customer ID (cus_xxx) for a user's
     * Connect account, creating a stub record if needed.
     *
     * @param userId           the primary key of the user to update
     * @param stripeCustomerId the Stripe Customer ID (e.g. {@code cus_xxxxx}) to store
     */
    void updatePlatformCustomerId(Long userId, String stripeCustomerId);

    /**
     * Projection used when syncing users to Stripe.
     *
     * @param userId    the database primary key of the user
     * @param email     the user's email address
     * @param country   the user's country (may be {@code null})
     * @param planCode  the code of the user's active subscription plan (e.g. FREE)
     */
    record UserStripeInfo(
            Long userId,
            String email,
            String country,
            String planCode
    ) {}
}
