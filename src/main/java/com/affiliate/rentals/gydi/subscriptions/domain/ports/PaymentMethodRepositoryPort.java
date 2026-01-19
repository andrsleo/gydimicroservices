package com.affiliate.rentals.gydi.subscriptions.domain.ports;

import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethodType;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for PaymentMethod aggregate.
 *
 * <p>
 * This interface defines the contract for payment method persistence operations
 * following the hexagonal architecture pattern.
 * </p>
 *
 * @author GYDI Development Team
 */
public interface PaymentMethodRepositoryPort {

    /**
     * Persists a new payment method or updates an existing one.
     *
     * @param paymentMethod the payment method to save
     * @return the saved payment method with generated ID if it was new
     */
    PaymentMethod save(PaymentMethod paymentMethod);

    /**
     * Finds a payment method by its unique identifier.
     *
     * @param id the payment method ID
     * @return an Optional containing the payment method if found, empty otherwise
     */
    Optional<PaymentMethod> findById(Long id);

    /**
     * Finds a payment method by its gateway token.
     *
     * @param gatewayToken the payment gateway token
     * @return an Optional containing the payment method if found, empty otherwise
     */
    Optional<PaymentMethod> findByGatewayToken(String gatewayToken);

    /**
     * Retrieves all active payment methods for a specific user.
     *
     * @param userId the user ID
     * @return a list of active payment methods for the user
     */
    List<PaymentMethod> findByUserId(Long userId);

    /**
     * Retrieves all active payment methods for a specific user.
     *
     * @param userId the user ID
     * @return a list of active payment methods
     */
    List<PaymentMethod> findActiveByUserId(Long userId);

    /**
     * Finds the default payment method for a user.
     *
     * @param userId the user ID
     * @return an Optional containing the default payment method if found, empty
     *         otherwise
     */
    Optional<PaymentMethod> findDefaultByUserId(Long userId);

    /**
     * Retrieves all payment methods of a specific type for a user.
     *
     * @param userId     the user ID
     * @param methodType the payment method type
     * @return a list of payment methods matching the criteria
     */
    List<PaymentMethod> findByUserIdAndType(Long userId, PaymentMethodType methodType);

    /**
     * Retrieves all payment methods expiring in the current month.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @return a list of expiring payment methods
     */
    List<PaymentMethod> findExpiringInMonth(int year, int month);

    /**
     * Checks if a user has at least one active payment method.
     *
     * @param userId the user ID
     * @return true if user has an active payment method, false otherwise
     */
    boolean hasActivePaymentMethod(Long userId);

    /**
     * Deletes a payment method by its ID.
     *
     * @param id the ID of the payment method to delete
     */
    void deleteById(Long id);

    /**
     * Counts active payment methods for a user.
     *
     * @param userId the user ID
     * @return the number of active payment methods
     */
    long countActiveByUserId(Long userId);
}
