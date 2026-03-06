package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Stripe-specific user queries.
 *
 * <p>Post V95 migration: stripe_customer_id has been removed from users.users.
 * The query now finds users who do not yet have a Stripe Platform Customer ID
 * in their commissions.stripe_connect_accounts record.</p>
 *
 * @author GYDI Development Team
 */
@Repository
public interface UserStripeJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Returns active users that still lack a Stripe Platform Customer ID.
     *
     * <p>Checks commissions.stripe_connect_accounts.stripe_platform_customer_id.
     * Users with no Connect account record at all are also included.</p>
     *
     * <p>Projection columns: [userId (Long), email (String), country (String|null),
     * planCode (String)]</p>
     */
    @Query(
        value = """
            SELECT u.id        AS userId,
                   u.email     AS email,
                   up.country  AS country,
                   u.active_plan AS planCode
            FROM   users.users u
            LEFT JOIN users.user_profile up ON up.user_id = u.id
            WHERE  u.is_active = TRUE
              AND  NOT EXISTS (
                SELECT 1
                FROM commissions.stripe_connect_accounts sca
                WHERE sca.user_id = u.id
                  AND sca.stripe_platform_customer_id IS NOT NULL
              )
            ORDER BY u.id
            """,
        nativeQuery = true
    )
    List<Object[]> findActiveUsersWithoutPlatformCustomerRaw();
}
