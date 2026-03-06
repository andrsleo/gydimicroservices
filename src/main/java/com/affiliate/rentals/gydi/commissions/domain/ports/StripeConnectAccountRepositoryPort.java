package com.affiliate.rentals.gydi.commissions.domain.ports;

import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port for Stripe Connect Account persistence.
 * <p>
 * Hexagonal Architecture: This is a port (interface) defined in the domain layer.
 * The actual implementation (adapter) lives in the infrastructure layer.
 * </p>
 */
public interface StripeConnectAccountRepositoryPort {

    /**
     * Saves a Stripe Connect Account (create or update).
     *
     * @param account the account to save
     * @return the saved account with ID
     */
    StripeConnectAccount save(StripeConnectAccount account);

    /**
     * Finds a Connect Account by user ID.
     *
     * @param userId the user ID
     * @return the account, if exists
     */
    Optional<StripeConnectAccount> findByUserId(Long userId);

    /**
     * Finds a Connect Account by Stripe Account ID.
     *
     * @param stripeAccountId the Stripe account ID
     * @return the account, if exists
     */
    Optional<StripeConnectAccount> findByStripeAccountId(String stripeAccountId);

    /**
     * Checks if a user has a Connect Account.
     *
     * @param userId the user ID
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Returns the Stripe Platform Customer ID (cus_xxx) for the given user.
     *
     * @param userId the user ID
     * @return the platform customer ID if present
     */
    Optional<String> findPlatformCustomerIdByUserId(Long userId);

    /**
     * Persists the Stripe Platform Customer ID for the user's Connect account.
     * Creates a stub Connect account record if the user does not have one yet.
     *
     * @param userId           the user ID
     * @param stripeCustomerId the Stripe Customer ID (cus_xxx)
     */
    void updatePlatformCustomerId(Long userId, String stripeCustomerId);

    /**
     * Returns all Connect accounts that have a stub stripe_account_id (pending_N).
     * These users need to complete real Stripe Connect onboarding.
     */
    List<StripeConnectAccount> findPendingConnectAccounts();
}
