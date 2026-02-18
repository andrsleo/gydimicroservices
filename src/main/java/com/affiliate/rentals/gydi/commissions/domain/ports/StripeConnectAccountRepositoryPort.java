package com.affiliate.rentals.gydi.commissions.domain.ports;

import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;

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
}
