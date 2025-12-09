package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.event.BookingCreatedEvent;
import com.affiliate.rentals.gydi.bookings.domain.exception.DateRangeNotAvailableException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.ClientInfo;
import com.affiliate.rentals.gydi.bookings.domain.model.DateRange;
import com.affiliate.rentals.gydi.bookings.domain.port.in.CreateBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.out.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Use Case: Create a new booking request.
 * <p>
 * Flow:
 * 1. Validate referral link exists and is active (future enhancement)
 * 2. Validate property exists (future enhancement)
 * 3. Check date availability
 * 4. Create booking in REQUEST status
 * 5. Persist booking
 * 6. Publish BookingCreatedEvent
 * </p>
 */
@Service
@Transactional
public class CreateBookingUseCaseImpl implements CreateBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateBookingUseCaseImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookingRepositoryPort bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService jwtReferralTokenService;
    private final com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository referralLinkRepository;

    public CreateBookingUseCaseImpl(BookingRepositoryPort bookingRepository,
            ApplicationEventPublisher eventPublisher,
            com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService jwtReferralTokenService,
            com.affiliate.rentals.gydi.referrals.domain.port.ReferralLinkRepository referralLinkRepository) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.jwtReferralTokenService = jwtReferralTokenService;
        this.referralLinkRepository = referralLinkRepository;
    }

    @Override
    public BookingResponse execute(CreateBookingCommand command) {
        log.info("Creating booking request for property {} via referral link ID/token {}",
                command.propertyId(), command.referralLinkId());

        // 0. Resolve referral link ID (handle both JWT token and numeric ID)
        Long actualReferralLinkId = resolveReferralLinkId(command.referralLinkId(), command.propertyId());
        log.debug("Resolved referral link ID: {}", actualReferralLinkId);

        // 1. Create domain value objects
        DateRange dateRange = new DateRange(command.startDate(), command.endDate());
        ClientInfo clientInfo = new ClientInfo(
                command.clientEmail(),
                command.clientPhone(),
                command.clientFirstName(),
                command.clientLastName());

        // 2. Check property availability for requested dates
        if (!bookingRepository.isPropertyAvailable(command.propertyId(), dateRange)) {
            log.warn("Property {} not available for dates {} to {}",
                    command.propertyId(), command.startDate(), command.endDate());
            throw new DateRangeNotAvailableException(command.startDate(), command.endDate());
        }

        // 3. Create booking (domain logic) with RESOLVED referral link ID
        Booking booking = Booking.createRequest(
                actualReferralLinkId,
                command.propertyId(),
                dateRange,
                clientInfo);

        // 4. Persist booking
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking {} created successfully in REQUEST status. Client: {}",
                savedBooking.getId(), clientInfo.maskedEmail());

        // 5. Publish domain event
        BookingCreatedEvent event = new BookingCreatedEvent(
                savedBooking.getId(),
                savedBooking.getReferralLinkId(),
                savedBooking.getPropertyId(),
                savedBooking.getClientInfo().getEmail(),
                savedBooking.getDateRange().getStartDate(),
                savedBooking.getDateRange().getEndDate(),
                savedBooking.getCreatedAt());
        eventPublisher.publishEvent(event);

        log.debug("BookingCreatedEvent published for booking {}", savedBooking.getId());

        // 6. Map to response
        return mapToResponse(savedBooking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getReferralLinkId(),
                booking.getPropertyId(),
                booking.getDateRange().getStartDate(),
                booking.getDateRange().getEndDate(),
                booking.getClientInfo().getEmail(),
                booking.getClientInfo().fullName(),
                booking.getStatus().name(),
                booking.getCreatedAt().format(DATE_TIME_FORMATTER));
    }

    /**
     * Resolves the referral link ID from either a JWT token or numeric string.
     * 
     * @param referralLinkIdOrToken Either a numeric ID (e.g. "123") or JWT token
     * @param propertyId            The property ID (used for validation when
     *                              resolving JWT)
     * @return The actual referral link ID
     */
    private Long resolveReferralLinkId(String referralLinkIdOrToken, Long propertyId) {
        // Try parsing as numeric ID first (system link case)
        try {
            return Long.parseLong(referralLinkIdOrToken);
        } catch (NumberFormatException e) {
            // Not a numeric ID, must be a JWT token
            log.debug("referralLinkId is not numeric, attempting JWT token resolution");
        }

        // Decode JWT token to get affiliate and property info
        try {
            com.affiliate.rentals.gydi.shared.security.JwtReferralTokenService.ReferralTokenData tokenData = jwtReferralTokenService
                    .validateAndExtract(referralLinkIdOrToken);

            log.debug("JWT token decoded - affiliateId: {}, propertyId: {}",
                    tokenData.userId(), tokenData.propertyId());

            // Validate property ID matches
            if (!tokenData.propertyId().equals(propertyId)) {
                log.warn("Property ID mismatch. Token property: {}, Booking property: {}",
                        tokenData.propertyId(), propertyId);
                throw new IllegalArgumentException(
                        "Referral link token property ID does not match booking property ID");
            }

            // Find the active referral link for this affiliate and property
            com.affiliate.rentals.gydi.referrals.domain.model.ReferralLink link = referralLinkRepository
                    .findActiveLink(tokenData.userId(), tokenData.propertyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No active referral link found for affiliate " + tokenData.userId() +
                                    " and property " + tokenData.propertyId()));

            log.info("Resolved JWT token to referral link ID: {} (affiliate: {})",
                    link.getId(), tokenData.userId());

            return link.getId();

        } catch (Exception e) {
            log.error("Failed to resolve referral link ID from token: {}", e.getMessage());
            throw new IllegalArgumentException(
                    "Invalid referral link ID or token: " + e.getMessage(), e);
        }
    }
}
