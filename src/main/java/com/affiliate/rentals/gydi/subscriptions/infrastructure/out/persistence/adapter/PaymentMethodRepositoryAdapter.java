package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity.PaymentMethodTypeEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper.PaymentMethodEntityMapper;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository.PaymentMethodJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of the PaymentMethodRepositoryPort using JPA.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with the infrastructure persistence layer.
 * It uses JPA repositories and mappers to persist and retrieve PaymentMethod domain models.</p>
 *
 * @author GYDI Development Team
 * @see PaymentMethodRepositoryPort
 * @see PaymentMethodJpaRepository
 * @see PaymentMethodEntityMapper
 */
@Component
public class PaymentMethodRepositoryAdapter implements PaymentMethodRepositoryPort {

    private final PaymentMethodJpaRepository paymentMethodJpaRepository;
    private final PaymentMethodEntityMapper mapper;

    /**
     * Constructs a new PaymentMethodRepositoryAdapter with required dependencies.
     *
     * @param paymentMethodJpaRepository the JPA repository for payment methods
     * @param mapper the mapper for PaymentMethod-PaymentMethodEntity conversion
     */
    public PaymentMethodRepositoryAdapter(
            PaymentMethodJpaRepository paymentMethodJpaRepository,
            PaymentMethodEntityMapper mapper) {
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodEntity entity = mapper.toEntity(paymentMethod);
        PaymentMethodEntity savedEntity = paymentMethodJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentMethod> findById(Long id) {
        return paymentMethodJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentMethod> findByGatewayToken(String gatewayToken) {
        return paymentMethodJpaRepository.findByGatewayToken(gatewayToken)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findByUserId(Long userId) {
        return paymentMethodJpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findActiveByUserId(Long userId) {
        return paymentMethodJpaRepository.findActiveByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentMethod> findDefaultByUserId(Long userId) {
        return paymentMethodJpaRepository.findDefaultByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findByUserIdAndType(Long userId, PaymentMethodType methodType) {
        PaymentMethodTypeEntity entityType = PaymentMethodTypeEntity.valueOf(methodType.name());
        return paymentMethodJpaRepository.findByUserIdAndMethodType(userId, entityType).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> findExpiringInMonth(int year, int month) {
        return paymentMethodJpaRepository.findExpiringInMonth(year, month).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActivePaymentMethod(Long userId) {
        return paymentMethodJpaRepository.hasActivePaymentMethod(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        paymentMethodJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByUserId(Long userId) {
        return paymentMethodJpaRepository.countActiveByUserId(userId);
    }
}
