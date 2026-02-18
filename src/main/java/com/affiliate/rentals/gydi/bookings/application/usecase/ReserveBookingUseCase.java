package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.ReserveBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Use Case: Reserve a booking (REQUEST → RESERVED).
 * <p>
 * Business Flow:
 * 1. Load booking by ID
 * 2. Validate admin authorization
 * 3. Call booking.reserve() with Airbnb code and amount
 * 4. Persist updated booking
 * 5. Return booking DTO
 * </p>
 */
@Service
public class ReserveBookingUseCase {
    private static final Logger log = LoggerFactory.getLogger(ReserveBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;
    private final com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort userSubscriptionPort;

    public ReserveBookingUseCase(
            BookingRepositoryPort bookingRepository,
            BookingMapper bookingMapper,
            com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort userSubscriptionPort) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.bookingMapper = Objects.requireNonNull(bookingMapper);
        this.userSubscriptionPort = Objects.requireNonNull(userSubscriptionPort);
    }

    /**
     * Executes the use case.
     *
     * @param bookingId booking ID to reserve
     * @param request   reserve booking command
     * @return updated booking DTO
     * @throws BookingNotFoundException                if booking not found
     * @throws InvalidBookingStatusTransitionException if booking cannot be reserved
     */
    @Transactional
    public BookingDto execute(Long bookingId, ReserveBookingRequest request) {
        log.info("Reserving booking {} by admin {}", bookingId, request.adminId());

        // Load booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Update dates if provided (dates may have changed in Airbnb)
        if (request.checkInDate() != null && request.checkOutDate() != null) {
            log.info("Updating booking dates: check-in={}, check-out={}",
                    request.checkInDate(), request.checkOutDate());
            booking.updateDates(request.checkInDate(), request.checkOutDate());
        }

        // Create Money value object
        Money totalAmount = Money.of(request.totalAmount(), request.currency());

        // Fetch commission rates (Snapshot at reservation time)
        // 1. Host commission rate
        Long hostId = bookingRepository.findHostIdByPropertyId(booking.getPropertyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Property " + booking.getPropertyId() + " has no associated host"));

        BigDecimal hostCommissionRate = userSubscriptionPort.getHostCommissionRate(hostId);
        log.info("Snapshotted host commission rate: {} for host: {}", hostCommissionRate, hostId);

        // 2. Affiliate commission rate (if applicable)
        BigDecimal affiliateCommissionRate = null;
        if (booking.getReferralLinkId() != null) {
            Long affiliateId = bookingRepository.findAffiliateIdByReferralLinkId(booking.getReferralLinkId())
                    .orElse(null);

            if (affiliateId != null) {
                affiliateCommissionRate = userSubscriptionPort.getAffiliateCommissionRate(affiliateId);
                log.info("Snapshotted affiliate commission rate: {} for affiliate: {}", affiliateCommissionRate,
                        affiliateId);
            }
        }

        // Reserve booking (domain logic)
        booking.reserve(request.airbnbConfirmationCode(), totalAmount, hostCommissionRate, affiliateCommissionRate,
                request.adminId());

        // Persist
        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} reserved successfully with Airbnb code: {}",
                bookingId, request.airbnbConfirmationCode());

        // Map to DTO
        return bookingMapper.toDto(updated);
    }
}
