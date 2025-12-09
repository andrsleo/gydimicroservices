package com.affiliate.rentals.gydi.payment.domain.port.out;

import com.affiliate.rentals.gydi.payment.domain.model.PaymentBooking;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for payment persistence operations.
 * <p>
 * This is a domain-level interface (port) that will be implemented
 * by the infrastructure layer (PaymentRepositoryAdapter).
 * </p>
 */
public interface PaymentRepositoryPort {

    /**
     * Saves a payment (create or update).
     */
    PaymentBooking save(PaymentBooking payment);

    /**
     * Finds payment by ID.
     */
    Optional<PaymentBooking> findById(Long id);

    /**
     * Finds payment by booking ID.
     */
    Optional<PaymentBooking> findByBookingId(Long bookingId);

    /**
     * Finds all payments for a specific user.
     */
    List<PaymentBooking> findByUserId(Long userId);

    /**
     * Finds all payments for a referral link.
     */
    List<PaymentBooking> findByReferralLinkId(Long referralLinkId);

    /**
     * Finds payments by status.
     */
    List<PaymentBooking> findByStatus(String status);

    /**
     * Checks if payment exists for booking ID.
     */
    boolean existsByBookingId(Long bookingId);
}
