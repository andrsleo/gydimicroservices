package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Get bookings for an affiliate.
 * <p>
 * Returns all bookings generated through the affiliate's referral links.
 * </p>
 */
@Service
public class GetAffiliateBookingsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final BookingMapper bookingMapper;

    public GetAffiliateBookingsUseCase(
        BookingRepositoryPort bookingRepository,
        ReferralLinkRepository referralLinkRepository,
        BookingMapper bookingMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.referralLinkRepository = referralLinkRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Executes the use case to get all bookings for an affiliate.
     *
     * @param affiliateId the affiliate's user ID
     * @return list of bookings generated through the affiliate's referral links
     */
    @Transactional(readOnly = true)
    public List<BookingDto> execute(Long affiliateId) {
        // Get all referral links for the affiliate
        List<ReferralLink> referralLinks = referralLinkRepository.findByAffiliateId(affiliateId);

        // Get all referral link IDs
        List<Long> referralLinkIds = referralLinks.stream()
            .map(ReferralLink::getId)
            .collect(Collectors.toList());

        // If affiliate has no referral links, return empty list
        if (referralLinkIds.isEmpty()) {
            return List.of();
        }

        // Get all bookings for those referral links
        List<Booking> bookings = referralLinkIds.stream()
            .flatMap(linkId -> bookingRepository.findByReferralLinkId(linkId).stream())
            .collect(Collectors.toList());

        // Convert to DTOs
        return bookings.stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }
}
