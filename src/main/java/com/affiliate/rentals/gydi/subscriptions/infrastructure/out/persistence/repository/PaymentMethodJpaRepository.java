package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PaymentMethodEntity.PaymentMethodTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for PaymentMethodEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for payment methods.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to payment method management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * PaymentMethodRepositoryAdapter to implement the PaymentMethodRepositoryPort.</p>
 *
 * @author GYDI Development Team
 * @see PaymentMethodEntity
 */
@Repository
public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethodEntity, Long> {

    /**
     * Finds a payment method by its gateway token.
     *
     * @param gatewayToken the payment gateway token
     * @return an Optional containing the payment method if found, empty otherwise
     */
    Optional<PaymentMethodEntity> findByGatewayToken(String gatewayToken);

    /**
     * Finds all payment methods for a specific user.
     *
     * @param userId the user ID
     * @return a list of payment methods for the user
     */
    List<PaymentMethodEntity> findByUserId(Long userId);

    /**
     * Finds all active payment methods for a specific user.
     * Uses status field instead of deprecated isActive flag.
     *
     * @param userId the user ID
     * @return a list of active payment methods (status = ACTIVE)
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.userId = :userId AND p.status = 'ACTIVE' ORDER BY p.isDefault DESC, p.createdAt DESC")
    List<PaymentMethodEntity> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Finds the default payment method for a user.
     * Uses status field instead of deprecated isActive flag.
     *
     * @param userId the user ID
     * @return an Optional containing the default payment method if found, empty otherwise
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.userId = :userId AND p.isDefault = true AND p.status = 'ACTIVE'")
    Optional<PaymentMethodEntity> findDefaultByUserId(@Param("userId") Long userId);

    /**
     * Finds all payment methods of a specific type for a user.
     *
     * @param userId the user ID
     * @param methodType the payment method type
     * @return a list of payment methods matching the criteria
     */
    List<PaymentMethodEntity> findByUserIdAndMethodType(Long userId, PaymentMethodTypeEntity methodType);

    /**
     * Finds all payment methods expiring in a specific month.
     * Uses status field instead of deprecated isActive flag.
     *
     * @param year the year
     * @param month the month (1-12)
     * @return a list of expiring payment methods (status = ACTIVE)
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.cardExpYear = :year AND p.cardExpMonth = :month AND p.status = 'ACTIVE'")
    List<PaymentMethodEntity> findExpiringInMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Checks if a user has at least one active payment method.
     * Uses status field instead of deprecated isActive flag.
     *
     * @param userId the user ID
     * @return true if user has an active payment method (status = ACTIVE), false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaymentMethodEntity p WHERE p.userId = :userId AND p.status = 'ACTIVE'")
    boolean hasActivePaymentMethod(@Param("userId") Long userId);

    /**
     * Counts active payment methods for a user.
     * Uses status field instead of deprecated isActive flag.
     *
     * @param userId the user ID
     * @return the number of active payment methods (status = ACTIVE)
     */
    @Query("SELECT COUNT(p) FROM PaymentMethodEntity p WHERE p.userId = :userId AND p.status = 'ACTIVE'")
    long countActiveByUserId(@Param("userId") Long userId);
}
