package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.StripeConnectAccountEntity;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper.StripeConnectAccountEntityMapper;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository.StripeConnectAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Repository Adapter for Stripe Connect Account persistence.
 * <p>
 * Hexagonal Architecture: Implements the port (interface) defined in domain layer.
 * Adapts JPA repository to domain repository interface.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class StripeConnectAccountRepositoryAdapter implements StripeConnectAccountRepositoryPort {

    private final StripeConnectAccountJpaRepository jpaRepository;
    private final StripeConnectAccountEntityMapper mapper;

    @Override
    public StripeConnectAccount save(StripeConnectAccount account) {
        StripeConnectAccountEntity entity = mapper.toEntity(account);
        StripeConnectAccountEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<StripeConnectAccount> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<StripeConnectAccount> findByStripeAccountId(String stripeAccountId) {
        return jpaRepository.findByStripeAccountId(stripeAccountId)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
