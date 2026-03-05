package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Stripe-specific user queries.
 *
 * <p>This repository exposes only the two narrow queries needed by
 * {@link com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter.UserStripeAdapter}
 * to support the startup Stripe customer synchronisation task.  Both queries
 * operate directly on the {@code users.users} table and therefore use native
 * SQL so they can reference the {@code is_active} column, which is present in
 * the database schema but intentionally not mapped on {@link UserEntity}.</p>
 *
 * @author GYDI Development Team
 */
@Repository
public interface UserStripeJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Returns a raw projection of every active user that still lacks a Stripe
     * Customer ID.
     *
     * <p>The projection columns map to
     * {@link com.affiliate.rentals.gydi.users.domain.ports.UserStripePort.UserStripeInfo}
     * in the following order: {@code userId}, {@code email}, {@code country},
     * {@code planCode}.</p>
     *
     * <p>The {@code country} column comes from the {@code users.user_profile}
     * table via a LEFT JOIN so that users without a profile are still included;
     * in that case the value will be {@code null}.</p>
     *
     * @return a list of Object arrays, never {@code null}; each entry contains
     *         [userId (Long), email (String), country (String|null),
     *          planCode (String)]
     */
    @Query(
        value = """
            SELECT u.id      AS userId,
                   u.email   AS email,
                   up.country AS country,
                   u.active_plan AS planCode
            FROM   users.users u
            LEFT JOIN users.user_profile up ON up.user_id = u.id
            WHERE  u.stripe_customer_id IS NULL
              AND  u.is_active = TRUE
            ORDER BY u.id
            """,
        nativeQuery = true
    )
    List<Object[]> findActiveUsersWithoutStripeCustomerRaw();

    /**
     * Sets the Stripe Customer ID for one user, but only when that field is
     * still {@code NULL}.
     *
     * <p>The additional {@code AND stripe_customer_id IS NULL} guard makes the
     * update idempotent: if a concurrent thread or a previous run already set
     * the value, this statement is a no-op and returns 0 rows updated.</p>
     *
     * @param userId           the user's primary key
     * @param stripeCustomerId the Stripe Customer ID to persist (e.g.
     *                         {@code cus_xxxxx})
     */
    @Modifying
    @Query(
        value = """
            UPDATE users.users
            SET    stripe_customer_id = :stripeCustomerId
            WHERE  id                 = :userId
              AND  stripe_customer_id IS NULL
            """,
        nativeQuery = true
    )
    void updateStripeCustomerIdIfNull(
            @Param("userId") Long userId,
            @Param("stripeCustomerId") String stripeCustomerId
    );
}
