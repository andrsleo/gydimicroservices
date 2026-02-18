package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.StripeConnectAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
