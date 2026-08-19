package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Get booking by ID with ownership validation.
 * <p>
 * <strong>SECURITY FIX:</strong> Validates that the requesting user owns the booking
 * (either as the affiliate who generated the referral link or as the host who owns the property).
 * ADMIN users can access all bookings.
 * </p>
 */
@Service
public class GetBookingByIdUseCase {
    private static final Logger logger = LoggerFactory.getLogger(GetBookingByIdUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;
    private final ReferralLinkRepository referralLinkRepository;
    private final PropertyRepositoryPort propertyRepository;

    public GetBookingByIdUseCase(
        BookingRepositoryPort bookingRepository,
        BookingMapper bookingMapper,
        ReferralLinkRepository referralLinkRepository,
        PropertyRepositoryPort propertyRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.referralLinkRepository = referralLinkRepository;
        this.propertyRepository = propertyRepository;
    }

    /**
     * Executes the use case to get a booking by ID.
     * <p>
     * <strong>SECURITY:</strong> Validates ownership before returning booking data.
     * </p>
     *
     * @param bookingId the booking ID
     * @param requestingUserId the ID of the user making the request
     * @param isAdmin whether the requesting user is an ADMIN
     * @return the booking DTO if access is allowed
     * @throws BookingNotFoundException if booking doesn't exist
     * @throws BookingAccessDeniedException if user doesn't own the booking
     */
    @Transactional(readOnly = true)
    public BookingDto execute(Long bookingId, Long requestingUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // ✅ SECURITY VALIDATION: Check ownership
        if (!isAdmin && !canAccessBooking(requestingUserId, booking)) {
            logger.warn(
                "Security violation: User {} attempted to access booking {} without ownership",
                requestingUserId,
                bookingId
            );
            throw new BookingAccessDeniedException(
                "You do not have permission to view this booking",
                bookingId,
                requestingUserId
            );
        }

        return bookingMapper.toDto(booking);
    }

    /**
     * Checks if the user can access the booking.
     * <p>
     * Access is allowed if the user is:
     * - The affiliate who owns the referral link, OR
     * - The host who owns the property
     * </p>
     *
     * @param userId the requesting user's ID
     * @param booking the booking to check
     * @return true if user can access, false otherwise
     */
    private boolean canAccessBooking(Long userId, Booking booking) {
        // Check if user is the affiliate (owns the referral link)
        boolean isAffiliate = referralLinkRepository.findById(booking.getReferralLinkId())
            .map(referralLink -> referralLink.getAffiliateId().equals(userId))
            .orElse(false);

        if (isAffiliate) {
            return true;
        }

        // Check if user is the host (owns the property)
        boolean isHost = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
            .map(property -> property.getHostId().equals(userId))
            .orElse(false);

        return isHost;
    }
}
