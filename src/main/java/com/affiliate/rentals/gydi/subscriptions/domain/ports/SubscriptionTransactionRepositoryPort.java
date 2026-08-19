package com.affiliate.rentals.gydi.subscriptions.domain.ports;

import com.affiliate.rentals.gydi.subscriptions.domain.model.SubscriptionTransaction;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionStatus;
import com.affiliate.rentals.gydi.subscriptions.domain.model.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for SubscriptionTransaction aggregate.
 *
 * <p>This interface defines the contract for subscription transaction persistence operations
 * following the hexagonal architecture pattern.</p>
 *
 * @author GYDI Development Team
 */
public interface SubscriptionTransactionRepositoryPort {

    /**
     * Persists a new transaction or updates an existing one.
     *
     * @param transaction the transaction to save
     * @return the saved transaction with generated ID if it was new
     */
    SubscriptionTransaction save(SubscriptionTransaction transaction);

    /**
     * Finds a transaction by its unique identifier.
     *
     * @param id the transaction ID
     * @return an Optional containing the transaction if found, empty otherwise
     */
    Optional<SubscriptionTransaction> findById(Long id);

    /**
     * Finds a transaction by Stripe Payment Intent ID.
     *
     * @param stripePaymentIntentId the Stripe Payment Intent ID
     * @return an Optional containing the transaction if found, empty otherwise
     */
    Optional<SubscriptionTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Retrieves all transactions for a specific user subscription.
     *
     * @param userSubscriptionId the user subscription ID
     * @return a list of transactions, ordered by creation date descending
     */
    List<SubscriptionTransaction> findByUserSubscriptionId(Long userSubscriptionId);

    /**
     * Retrieves all transactions with a specific status.
     *
     * @param status the transaction status
     * @return a list of transactions with the given status
     */
    List<SubscriptionTransaction> findByStatus(TransactionStatus status);

    /**
     * Retrieves all transactions of a specific type.
     *
     * @param type the transaction type
     * @return a list of transactions of the given type
     */
    List<SubscriptionTransaction> findByType(TransactionType type);

    /**
     * Retrieves all pending or processing transactions older than a given date.
     *
     * @param olderThan the date threshold
     * @return a list of stale transactions that may need attention
     */
    List<SubscriptionTransaction> findStaleTransactions(LocalDateTime olderThan);

    /**
     * Retrieves all successful transactions for a subscription within a date range.
     *
     * @param userSubscriptionId the user subscription ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return a list of successful transactions in the date range
     */
    List<SubscriptionTransaction> findSuccessfulTransactionsBetween(
        Long userSubscriptionId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Retrieves the most recent transaction for a user subscription.
     *
     * @param userSubscriptionId the user subscription ID
     * @return an Optional containing the most recent transaction if found, empty otherwise
     */
    Optional<SubscriptionTransaction> findMostRecentByUserSubscriptionId(Long userSubscriptionId);

    /**
     * Counts transactions by status.
     *
     * @param status the transaction status
     * @return the number of transactions with the given status
     */
    long countByStatus(TransactionStatus status);

    /**
     * Counts successful transactions for a user subscription.
     *
     * @param userSubscriptionId the user subscription ID
     * @return the number of successful transactions
     */
    long countSuccessfulByUserSubscriptionId(Long userSubscriptionId);

    /**
     * Deletes a transaction by its ID.
     *
     * @param id the ID of the transaction to delete
     */
    void deleteById(Long id);
}
