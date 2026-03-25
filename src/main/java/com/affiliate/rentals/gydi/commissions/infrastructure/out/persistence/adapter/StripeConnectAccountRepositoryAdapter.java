package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.model.ConnectAccountType;
import com.affiliate.rentals.gydi.commissions.domain.model.StripeConnectAccount;
import com.affiliate.rentals.gydi.commissions.domain.ports.StripeConnectAccountRepositoryPort;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.entity.StripeConnectAccountEntity;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper.StripeConnectAccountEntityMapper;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository.StripeConnectAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findPlatformCustomerIdByUserId(Long userId) {
        return jpaRepository.findPlatformCustomerIdByUserId(userId);
    }

    @Override
    @Transactional
    public void updatePlatformCustomerId(Long userId, String stripeCustomerId) {
        if (jpaRepository.existsByUserId(userId)) {
            jpaRepository.updatePlatformCustomerIdIfNull(userId, stripeCustomerId);
        } else {
            // Create a stub Connect account record so the customer ID is not lost
            StripeConnectAccountEntity stub = StripeConnectAccountEntity.builder()
                .userId(userId)
                .stripeAccountId("pending_" + userId)
                .accountType(ConnectAccountType.EXPRESS)
                .onboardingCompleted(false)
                .detailsSubmitted(false)
                .payoutsEnabled(false)
                .chargesEnabled(false)
                .country("US")
                .verificationStatus("unverified")
                .stripePlatformCustomerId(stripeCustomerId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            jpaRepository.save(stub);
        }
    }

    @Override
    @Transactional
    public void upgradeStubAccount(Long userId, String realStripeAccountId) {
        jpaRepository.updateStripeAccountId(userId, realStripeAccountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StripeConnectAccount> findPendingConnectAccounts() {
        return jpaRepository.findPendingAccounts().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void savePayPalEmail(Long userId, String paypalEmail) {
        if (jpaRepository.existsByUserId(userId)) {
            jpaRepository.updatePayPalEmail(userId, paypalEmail);
        } else {
            // Create a stub account record so the PayPal email is not lost
            StripeConnectAccountEntity stub = StripeConnectAccountEntity.builder()
                .userId(userId)
                .stripeAccountId("pending_" + userId)
                .accountType(ConnectAccountType.EXPRESS)
                .onboardingCompleted(false)
                .detailsSubmitted(false)
                .payoutsEnabled(false)
                .chargesEnabled(false)
                .country("US")
                .verificationStatus("unverified")
                .paypalEmail(paypalEmail)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
            jpaRepository.save(stub);
        }
    }
}
