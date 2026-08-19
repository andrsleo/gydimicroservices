package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.UserSubscription;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.UserSubscriptionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.UserSubscriptionEntity.SubscriptionStatusEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper.UserSubscriptionEntityMapper;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository.UserSubscriptionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of the UserSubscriptionRepositoryPort using JPA.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with the infrastructure persistence layer.
 * It uses JPA repositories and mappers to persist and retrieve UserSubscription domain models.</p>
 *
 * @author GYDI Development Team
 * @see UserSubscriptionRepositoryPort
 * @see UserSubscriptionJpaRepository
 * @see UserSubscriptionEntityMapper
 */
@Component
public class UserSubscriptionRepositoryAdapter implements UserSubscriptionRepositoryPort {

    private final UserSubscriptionJpaRepository subscriptionJpaRepository;
    private final UserSubscriptionEntityMapper mapper;

    /**
     * Constructs a new UserSubscriptionRepositoryAdapter with required dependencies.
     *
     * @param subscriptionJpaRepository the JPA repository for subscriptions
     * @param mapper the mapper for UserSubscription-UserSubscriptionEntity conversion
     */
    public UserSubscriptionRepositoryAdapter(
            UserSubscriptionJpaRepository subscriptionJpaRepository,
            UserSubscriptionEntityMapper mapper) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserSubscription save(UserSubscription subscription) {
        UserSubscriptionEntity entity = mapper.toEntity(subscription);
        UserSubscriptionEntity savedEntity = subscriptionJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSubscription> findById(Long id) {
        return subscriptionJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSubscription> findByUserId(Long userId) {
        return subscriptionJpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return subscriptionJpaRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findByStatus(SubscriptionStatus status) {
        SubscriptionStatusEntity entityStatus = SubscriptionStatusEntity.valueOf(status.name());
        return subscriptionJpaRepository.findByStatus(entityStatus).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findByPlanId(Long planId) {
        return subscriptionJpaRepository.findByPlanId(planId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findExpiringBefore(LocalDateTime expirationDate) {
        return subscriptionJpaRepository.findExpiringBefore(expirationDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findActiveWithAutoRenewal() {
        return subscriptionJpaRepository.findActiveWithAutoRenewal().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findDueForBillingBefore(LocalDateTime billingDate) {
        return subscriptionJpaRepository.findDueForBillingBefore(billingDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long userId) {
        return subscriptionJpaRepository.hasActiveSubscription(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        subscriptionJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPlanId(Long planId) {
        return subscriptionJpaRepository.countByPlanId(planId);
    }
}
