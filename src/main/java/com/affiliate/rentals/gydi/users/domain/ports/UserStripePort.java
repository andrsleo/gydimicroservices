package com.affiliate.rentals.gydi.users.domain.ports;

import java.util.List;

/**
 * Port for Stripe-specific user persistence operations.
 *
 * <p>This interface defines the contract for querying users that need a Stripe
 * Customer ID and for updating them once the customer has been created in Stripe.
 * It is implemented by an adapter in the infrastructure layer, keeping the domain
 * free of any persistence or external-service concerns.</p>
 *
 * <p>The port is intentionally narrow: it exposes only the two operations required
 * by the {@code StripeCustomerSyncRunner} startup task, rather than reusing the
 * broader {@link UserRepositoryPort}. This respects the Interface Segregation
 * Principle and avoids polluting the generic repository contract with
 * Stripe-specific queries.</p>
 *
 * @author GYDI Development Team
 * @see com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter.UserStripeAdapter
 * @see com.affiliate.rentals.gydi.users.infrastructure.in.runner.StripeCustomerSyncRunner
 */
public interface UserStripePort {

    /**
     * Returns all active users that do not yet have a Stripe Customer ID.
     *
     * <p>Only users with {@code is_active = TRUE} and a {@code NULL}
     * {@code stripe_customer_id} are returned. The result is used by the
     * {@code StripeCustomerSyncRunner} to know which users still need to have a
     * Stripe Customer record created.</p>
     *
     * @return a list of {@link UserStripeInfo} projections; never {@code null},
     *         may be empty
     */
    List<UserStripeInfo> findActiveUsersWithoutStripeCustomer();

    /**
     * Persists the given Stripe Customer ID for a user.
     *
     * <p>The update is guarded with an additional {@code WHERE stripe_customer_id IS NULL}
     * predicate so that concurrent or repeated invocations cannot overwrite an ID that
     * was already set between the initial read and this write.</p>
     *
     * @param userId           the primary key of the user to update
     * @param stripeCustomerId the Stripe Customer ID (e.g. {@code cus_xxxxx}) to store
     */
    void updateStripeCustomerId(Long userId, String stripeCustomerId);

    /**
     * Projection used when syncing users to Stripe.
     *
     * <p>Contains only the fields required to call
     * {@link com.stripe.model.Customer#create} and to log meaningful context.
     * The {@code planCode} is stored as metadata in Stripe for reporting
     * purposes.</p>
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
