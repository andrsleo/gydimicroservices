package com.affiliate.rentals.gydi.payment.adapters.out.persistence.repository;

import com.affiliate.rentals.gydi.payment.adapters.out.persistence.entity.PaymentBookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for PaymentBookingEntity.
 */
@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentBookingEntity, Long> {

    /**
     * Finds payment by booking ID.
     */
    Optional<PaymentBookingEntity> findByBookingId(Long bookingId);

    /**
     * Checks if payment exists for booking ID.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Finds all payments for a user.
     */
    List<PaymentBookingEntity> findByUserId(Long userId);

    /**
     * Finds all payments for a referral link.
     */
    List<PaymentBookingEntity> findByReferralLinkId(Long referralLinkId);

    /**
     * Finds payments by status.
     */
    List<PaymentBookingEntity> findByStatus(PaymentBookingEntity.PaymentStatusEntity status);
}
