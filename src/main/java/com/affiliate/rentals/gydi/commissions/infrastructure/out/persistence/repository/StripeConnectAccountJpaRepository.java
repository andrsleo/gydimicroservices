package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.StripeConnectAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for StripeConnectAccountEntity.
 */
@Repository
public interface StripeConnectAccountJpaRepository extends JpaRepository<StripeConnectAccountEntity, Long> {

    /**
     * Finds Connect Account by user ID.
     */
    Optional<StripeConnectAccountEntity> findByUserId(Long userId);

    /**
     * Finds Connect Account by Stripe Account ID.
     */
    Optional<StripeConnectAccountEntity> findByStripeAccountId(String stripeAccountId);

    /**
     * Checks if user has a Connect Account.
     */
    boolean existsByUserId(Long userId);

    /**
     * Returns the platform customer ID for a user.
     */
    @Query(value = "SELECT stripe_platform_customer_id FROM commissions.stripe_connect_accounts WHERE user_id = :userId",
           nativeQuery = true)
    Optional<String> findPlatformCustomerIdByUserId(@Param("userId") Long userId);

    /**
     * Updates stripe_platform_customer_id if not already set.
     */
    @Modifying
    @Query(value = "UPDATE commissions.stripe_connect_accounts SET stripe_platform_customer_id = :customerId, updated_at = NOW() WHERE user_id = :userId AND stripe_platform_customer_id IS NULL",
           nativeQuery = true)
    void updatePlatformCustomerIdIfNull(@Param("userId") Long userId, @Param("customerId") String customerId);

    /**
     * Upgrades a stub stripe_account_id (pending_N) to a real acct_xxx.
     */
    @Modifying
    @Query(value = "UPDATE commissions.stripe_connect_accounts SET stripe_account_id = :realId, updated_at = NOW() WHERE user_id = :userId",
           nativeQuery = true)
    void updateStripeAccountId(@Param("userId") Long userId, @Param("realId") String realId);

    /**
     * Returns stub accounts created by migration (pending real Connect onboarding).
     */
    @Query(value = "SELECT * FROM commissions.stripe_connect_accounts WHERE stripe_account_id LIKE 'pending_%'",
           nativeQuery = true)
    List<StripeConnectAccountEntity> findPendingAccounts();
}
