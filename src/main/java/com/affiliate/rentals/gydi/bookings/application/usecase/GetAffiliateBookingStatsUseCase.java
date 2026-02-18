package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingStatsDto;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink;
import com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use Case: Get booking statistics for an affiliate.
 * <p>
 * Calculates total bookings, active bookings, and total revenue
 * from bookings generated through the affiliate's referral links.
 * </p>
 */
@Service
public class GetAffiliateBookingStatsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final ReferralLinkRepository referralLinkRepository;

    public GetAffiliateBookingStatsUseCase(
        BookingRepositoryPort bookingRepository,
        ReferralLinkRepository referralLinkRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.referralLinkRepository = referralLinkRepository;
    }

    /**
     * Executes the use case to get booking statistics for an affiliate.
     *
     * @param affiliateId the affiliate's user ID
     * @return booking statistics
     */
    @Transactional(readOnly = true)
    public BookingStatsDto execute(Long affiliateId) {
        // Get all referral links for the affiliate
        List<ReferralLink> referralLinks = referralLinkRepository.findByAffiliateId(affiliateId);

        // Get all referral link IDs
        List<Long> referralLinkIds = referralLinks.stream()
            .map(ReferralLink::getId)
            .collect(Collectors.toList());

        // If affiliate has no referral links, return empty stats
        if (referralLinkIds.isEmpty()) {
            return new BookingStatsDto(0, 0, BigDecimal.ZERO, "USD");
        }

        // Get all bookings for those referral links
        List<Booking> bookings = referralLinkIds.stream()
            .flatMap(linkId -> bookingRepository.findByReferralLinkId(linkId).stream())
            .collect(Collectors.toList());

        // Calculate stats
        int totalBookings = bookings.size();

        // Active bookings: RESERVED, IN_PROGRESS
        int activeBookings = (int) bookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.RESERVED ||
                             booking.getStatus() == BookingStatus.IN_PROGRESS)
            .count();

        // Total revenue: sum of all FINISHED bookings
        // Filter bookings that have totalAmount set (not null)
        BigDecimal totalRevenue = bookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.FINISHED)
            .map(Booking::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce((money1, money2) -> money1.add(money2))
            .map(money -> money.getAmount())
            .orElse(BigDecimal.ZERO);

        // Get currency from first booking with amount, default to USD
        String currency = bookings.stream()
            .map(Booking::getTotalAmount)
            .filter(Objects::nonNull)
            .findFirst()
            .map(money -> money.getCurrency())
            .orElse("USD");

        return new BookingStatsDto(totalBookings, activeBookings, totalRevenue, currency);
    }
}
