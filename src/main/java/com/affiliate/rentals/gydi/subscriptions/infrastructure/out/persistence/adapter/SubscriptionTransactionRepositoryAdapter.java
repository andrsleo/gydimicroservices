package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionTransaction;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionType;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.SubscriptionTransactionRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionStatusEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionTypeEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper.SubscriptionTransactionEntityMapper;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository.SubscriptionTransactionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of the SubscriptionTransactionRepositoryPort using JPA.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with the infrastructure persistence layer.
 * It uses JPA repositories and mappers to persist and retrieve SubscriptionTransaction domain models.</p>
 *
 * @author GYDI Development Team
 * @see SubscriptionTransactionRepositoryPort
 * @see SubscriptionTransactionJpaRepository
 * @see SubscriptionTransactionEntityMapper
 */
@Component
public class SubscriptionTransactionRepositoryAdapter implements SubscriptionTransactionRepositoryPort {

    private final SubscriptionTransactionJpaRepository transactionJpaRepository;
    private final SubscriptionTransactionEntityMapper mapper;

    /**
     * Constructs a new SubscriptionTransactionRepositoryAdapter with required dependencies.
     *
     * @param transactionJpaRepository the JPA repository for transactions
     * @param mapper the mapper for SubscriptionTransaction-SubscriptionTransactionEntity conversion
     */
    public SubscriptionTransactionRepositoryAdapter(
            SubscriptionTransactionJpaRepository transactionJpaRepository,
            SubscriptionTransactionEntityMapper mapper) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SubscriptionTransaction save(SubscriptionTransaction transaction) {
        SubscriptionTransactionEntity entity = mapper.toEntity(transaction);
        SubscriptionTransactionEntity savedEntity = transactionJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionTransaction> findById(Long id) {
        return transactionJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionTransaction> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return transactionJpaRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransaction> findByUserSubscriptionId(Long userSubscriptionId) {
        return transactionJpaRepository.findByUserSubscriptionId(userSubscriptionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransaction> findByStatus(TransactionStatus status) {
        TransactionStatusEntity entityStatus = TransactionStatusEntity.valueOf(status.name());
        return transactionJpaRepository.findByTransactionStatus(entityStatus).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransaction> findByType(TransactionType type) {
        TransactionTypeEntity entityType = TransactionTypeEntity.valueOf(type.name());
        return transactionJpaRepository.findByTransactionType(entityType).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransaction> findStaleTransactions(LocalDateTime olderThan) {
        return transactionJpaRepository.findStaleTransactions(olderThan).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionTransaction> findSuccessfulTransactionsBetween(
            Long userSubscriptionId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return transactionJpaRepository.findSuccessfulTransactionsBetween(
                userSubscriptionId, startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionTransaction> findMostRecentByUserSubscriptionId(Long userSubscriptionId) {
        return transactionJpaRepository.findMostRecentByUserSubscriptionId(userSubscriptionId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(TransactionStatus status) {
        TransactionStatusEntity entityStatus = TransactionStatusEntity.valueOf(status.name());
        return transactionJpaRepository.countByTransactionStatus(entityStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSuccessfulByUserSubscriptionId(Long userSubscriptionId) {
        return transactionJpaRepository.countSuccessfulByUserSubscriptionId(userSubscriptionId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        transactionJpaRepository.deleteById(id);
    }
}
