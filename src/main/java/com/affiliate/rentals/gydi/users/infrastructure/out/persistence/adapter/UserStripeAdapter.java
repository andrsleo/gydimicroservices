package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.UserStripeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adapter that implements {@link UserStripePort} using Spring Data JPA.
 *
 * <p>This is an outbound infrastructure adapter following hexagonal architecture
 * principles. It delegates persistence work to {@link UserStripeJpaRepository}
 * and maps the raw native-query result rows into
 * {@link UserStripePort.UserStripeInfo} records consumed by the application
 * layer.</p>
 *
 * <p>Both operations are executed inside transactions:
 * <ul>
 *   <li>{@code findActiveUsersWithoutStripeCustomer} — read-only transaction to
 *       minimise lock overhead.</li>
 *   <li>{@code updateStripeCustomerId} — writable transaction with an implicit
 *       {@code AND stripe_customer_id IS NULL} guard in the SQL to prevent
 *       accidental overwrites.</li>
 * </ul>
 * </p>
 *
 * @author GYDI Development Team
 * @see UserStripePort
 * @see UserStripeJpaRepository
 */
@Component
@RequiredArgsConstructor
public class UserStripeAdapter implements UserStripePort {

    private final UserStripeJpaRepository userStripeJpaRepository;

    /**
     * {@inheritDoc}
     *
     * <p>The native query returns columns in the order:
     * {@code [userId, email, country, planCode]}. Each column is cast to its
     * appropriate Java type and wrapped in a {@link UserStripePort.UserStripeInfo}
     * record.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserStripeInfo> findActiveUsersWithoutStripeCustomer() {
        return userStripeJpaRepository.findActiveUsersWithoutStripeCustomerRaw()
                .stream()
                .map(this::toUserStripeInfo)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateStripeCustomerId(Long userId, String stripeCustomerId) {
        userStripeJpaRepository.updateStripeCustomerIdIfNull(userId, stripeCustomerId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Maps one raw result row returned by the native query into a typed record.
     *
     * <p>Column positions mirror the SELECT list in
     * {@link UserStripeJpaRepository#findActiveUsersWithoutStripeCustomerRaw()}:
     * <ol>
     *   <li>index 0 — {@code userId}   (Number, cast to Long)</li>
     *   <li>index 1 — {@code email}    (String)</li>
     *   <li>index 2 — {@code country}  (String, nullable)</li>
     *   <li>index 3 — {@code planCode} (String)</li>
     * </ol>
     * </p>
     *
     * @param row a single Object[] from the native query
     * @return the corresponding {@link UserStripeInfo}
     */
    private UserStripeInfo toUserStripeInfo(Object[] row) {
        Long userId = ((Number) row[0]).longValue();
        String email = (String) row[1];
        String country = (String) row[2];
        String planCode = (String) row[3];
        return new UserStripeInfo(userId, email, country, planCode);
    }
}
