package com.affiliate.rentals.gydi.commissions.domain.ports;

import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for host commission repository.
 * <p>
 * Implemented by infrastructure layer adapter.
 * </p>
 */
public interface HostCommissionRepositoryPort {

    /**
     * Saves a host commission.
     */
    HostCommission save(HostCommission commission);

    /**
     * Finds commission by ID.
     */
    Optional<HostCommission> findById(Long id);

    /**
     * Finds commission by booking ID.
     */
    Optional<HostCommission> findByBookingId(Long bookingId);

    /**
     * Finds all commissions for a host.
     */
    List<HostCommission> findByHostId(Long hostId);

    /**
     * Finds commissions by host and status.
     */
    List<HostCommission> findByHostIdAndStatus(Long hostId, HostCommissionStatus status);

    /**
     * Finds all commissions with given status.
     */
    List<HostCommission> findByStatus(HostCommissionStatus status);

    /**
     * Finds commissions ready for retry (status = FAILED and next_retry_at <= now).
     */
    List<HostCommission> findCommissionsForRetry(LocalDateTime now);

    /**
     * Checks if commission exists for booking.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Deletes a commission by ID.
     */
    void deleteById(Long id);
}
