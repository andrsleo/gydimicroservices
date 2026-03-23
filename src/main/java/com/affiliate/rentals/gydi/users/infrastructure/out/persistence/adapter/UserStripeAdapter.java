package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserStripePort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.UserStripeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adapter that implements {@link UserStripePort}.
 *
 * <p>Post V95 migration: delegates customer ID storage to
 * {@link StripeConnectAccountRepositoryPort} instead of {@code users.users}.</p>
 *
 * @author GYDI Development Team
 */
@Component
@RequiredArgsConstructor
public class UserStripeAdapter implements UserStripePort {

    private final UserStripeJpaRepository userStripeJpaRepository;
    private final StripeConnectAccountRepositoryPort connectAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserStripeInfo> findActiveUsersWithoutPlatformCustomer() {
        return userStripeJpaRepository.findActiveUsersWithoutPlatformCustomerRaw()
                .stream()
                .map(this::toUserStripeInfo)
                .toList();
    }

    @Override
    @Transactional
    public void updatePlatformCustomerId(Long userId, String stripeCustomerId) {
        connectAccountRepository.updatePlatformCustomerId(userId, stripeCustomerId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private UserStripeInfo toUserStripeInfo(Object[] row) {
        Long userId = ((Number) row[0]).longValue();
        String email = (String) row[1];
        String country = (String) row[2];
        String planCode = (String) row[3];
        return new UserStripeInfo(userId, email, country, planCode);
    }
}
