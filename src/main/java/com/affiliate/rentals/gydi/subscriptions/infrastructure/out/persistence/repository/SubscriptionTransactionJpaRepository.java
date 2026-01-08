package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionStatusEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.SubscriptionTransactionEntity.TransactionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for SubscriptionTransactionEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for subscription transactions.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to transaction management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * SubscriptionTransactionRepositoryAdapter to implement the SubscriptionTransactionRepositoryPort.</p>
 *
 * @author GYDI Development Team
 * @see SubscriptionTransactionEntity
 */
@Repository
public interface SubscriptionTransactionJpaRepository extends JpaRepository<SubscriptionTransactionEntity, Long> {

    /**
     * Finds a transaction by Stripe Payment Intent ID.
     *
     * @param stripePaymentIntentId the Stripe Payment Intent ID
     * @return an Optional containing the transaction if found, empty otherwise
     */
    Optional<SubscriptionTransactionEntity> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Finds all transactions for a specific user subscription, ordered by creation date descending.
     *
     * @param userSubscriptionId the user subscription ID
     * @return a list of transactions
     */
    @Query("SELECT t FROM SubscriptionTransactionEntity t WHERE t.userSubscriptionId = :userSubscriptionId ORDER BY t.createdAt DESC")
    List<SubscriptionTransactionEntity> findByUserSubscriptionId(@Param("userSubscriptionId") Long userSubscriptionId);

    /**
     * Finds all transactions with a specific status.
     *
     * @param status the transaction status
     * @return a list of transactions with the given status
     */
    List<SubscriptionTransactionEntity> findByTransactionStatus(TransactionStatusEntity status);

    /**
     * Finds all transactions of a specific type.
     *
     * @param type the transaction type
     * @return a list of transactions of the given type
     */
    List<SubscriptionTransactionEntity> findByTransactionType(TransactionTypeEntity type);

    /**
     * Finds all pending or processing transactions older than a given date.
     *
     * @param olderThan the date threshold
     * @return a list of stale transactions that may need attention
     */
    @Query("SELECT t FROM SubscriptionTransactionEntity t WHERE (t.transactionStatus = 'PENDING' OR t.transactionStatus = 'PROCESSING') AND t.createdAt < :olderThan")
    List<SubscriptionTransactionEntity> findStaleTransactions(@Param("olderThan") LocalDateTime olderThan);

    /**
     * Finds all successful transactions for a subscription within a date range.
     *
     * @param userSubscriptionId the user subscription ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return a list of successful transactions in the date range
     */
    @Query("SELECT t FROM SubscriptionTransactionEntity t WHERE t.userSubscriptionId = :userSubscriptionId AND t.transactionStatus = 'COMPLETED' AND t.processedAt BETWEEN :startDate AND :endDate ORDER BY t.processedAt DESC")
    List<SubscriptionTransactionEntity> findSuccessfulTransactionsBetween(
        @Param("userSubscriptionId") Long userSubscriptionId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Finds the most recent transaction for a user subscription.
     *
     * @param userSubscriptionId the user subscription ID
     * @return an Optional containing the most recent transaction if found, empty otherwise
     */
    @Query("SELECT t FROM SubscriptionTransactionEntity t WHERE t.userSubscriptionId = :userSubscriptionId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<SubscriptionTransactionEntity> findMostRecentByUserSubscriptionId(@Param("userSubscriptionId") Long userSubscriptionId);

    /**
     * Counts transactions by status.
     *
     * @param status the transaction status
     * @return the number of transactions with the given status
     */
    long countByTransactionStatus(TransactionStatusEntity status);

    /**
     * Counts successful transactions for a user subscription.
     *
     * @param userSubscriptionId the user subscription ID
     * @return the number of successful transactions
     */
    @Query("SELECT COUNT(t) FROM SubscriptionTransactionEntity t WHERE t.userSubscriptionId = :userSubscriptionId AND t.transactionStatus = 'COMPLETED'")
    long countSuccessfulByUserSubscriptionId(@Param("userSubscriptionId") Long userSubscriptionId);
}
