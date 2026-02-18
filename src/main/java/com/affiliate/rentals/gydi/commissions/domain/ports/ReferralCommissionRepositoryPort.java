package com.affiliate.rentals.gydi.commissions.domain.ports;

import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.ReferralCommissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for referral commission repository.
 * <p>
 * Implemented by infrastructure layer adapter.
 * </p>
 */
public interface ReferralCommissionRepositoryPort {

    /**
     * Saves a referral commission.
     */
    ReferralCommission save(ReferralCommission commission);

    /**
     * Finds commission by ID.
     */
    Optional<ReferralCommission> findById(Long id);

    /**
     * Finds commission by booking ID.
     */
    Optional<ReferralCommission> findByBookingId(Long bookingId);

    /**
     * Finds all commissions for an affiliate.
     */
    List<ReferralCommission> findByAffiliateId(Long affiliateId);

    /**
     * Finds commissions by affiliate and status.
     */
    List<ReferralCommission> findByAffiliateIdAndStatus(Long affiliateId, ReferralCommissionStatus status);

    /**
     * Finds all commissions with given status.
     */
    List<ReferralCommission> findByStatus(ReferralCommissionStatus status);

    /**
     * Finds commissions ready for approval (dispute period ended).
     */
    List<ReferralCommission> findCommissionsReadyForApproval(LocalDateTime now);

    /**
     * Finds approved commissions ready for payment on given date.
     */
    List<ReferralCommission> findCommissionsForPayment(LocalDate paymentDate);

    /**
     * Checks if commission exists for booking.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Deletes a commission by ID.
     */
    void deleteById(Long id);
}
