package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.ReserveBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.PropertyCalendar;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Use Case: Reserve a booking (REQUEST → RESERVED).
 * <p>
 * Supports two confirmation flows:
 * - ADMIN flow: {@link #execute(Long, ReserveBookingRequest)} — ADMIN confirms with Airbnb code
 * - HOST flow: {@link #executeByHost(Long, Long)} — Property HOST confirms directly
 * </p>
 */
@Service
public class ReserveBookingUseCase {
    private static final Logger log = LoggerFactory.getLogger(ReserveBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;
    private final com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort userSubscriptionPort;
    private final UserRepositoryPort userRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final PropertyCalendarRepositoryPort calendarRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    public ReserveBookingUseCase(
            BookingRepositoryPort bookingRepository,
            BookingMapper bookingMapper,
            com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort userSubscriptionPort,
            UserRepositoryPort userRepository,
            PropertyRepositoryPort propertyRepository,
            PropertyCalendarRepositoryPort calendarRepository,
            EmailServicePort emailService,
            EmailTemplateService emailTemplateService) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.bookingMapper = Objects.requireNonNull(bookingMapper);
        this.userSubscriptionPort = Objects.requireNonNull(userSubscriptionPort);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
        this.calendarRepository = Objects.requireNonNull(calendarRepository);
        this.emailService = Objects.requireNonNull(emailService);
        this.emailTemplateService = Objects.requireNonNull(emailTemplateService);
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
                // Detect organic booking: affiliate is the system organic user
                Optional<Long> organicUserId = bookingRepository.findOrganicSystemUserId();
                boolean isOrganic = organicUserId.isPresent() && organicUserId.get().equals(affiliateId);

                if (isOrganic) {
                    // Organic booking: no affiliate commission, flat 15% host fee goes to platform
                    log.info("Organic booking detected (affiliate is system-organic user ID: {}). "
                            + "Setting affiliate commission rate to 0.00 and host commission rate to 0.15.",
                            affiliateId);
                    affiliateCommissionRate = BigDecimal.ZERO;
                    hostCommissionRate = new BigDecimal("0.15");
                } else {
                    affiliateCommissionRate = userSubscriptionPort.getAffiliateCommissionRate(affiliateId);
                    log.info("Snapshotted affiliate commission rate: {} for affiliate: {}",
                            affiliateCommissionRate, affiliateId);
                }
            }
        }

        // Reserve booking (domain logic)
        booking.reserve(request.airbnbConfirmationCode(), totalAmount, hostCommissionRate, affiliateCommissionRate,
                request.adminId());

        // Persist
        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} reserved successfully with Airbnb code: {}",
                bookingId, request.airbnbConfirmationCode());

        // Fire-and-forget: send booking reserved email to host
        sendBookingReservedEmail(updated, hostId);

        // Map to DTO
        return bookingMapper.toDto(updated);
    }

    /**
     * HOST confirmation flow: HOST confirms booking directly (REQUEST → RESERVED).
     * <p>
     * Business Flow:
     * 1. Load booking by ID
     * 2. Verify that the requesting user is the host of the property
     * 3. Snapshot commission rates
     * 4. Call booking.reserve() — uses "HOST_CONFIRMED" as confirmation code
     * 5. Block booking dates in property calendar
     * 6. Persist and return DTO
     * </p>
     *
     * @param bookingId        booking ID to confirm
     * @param requestingUserId ID of the host confirming the booking
     * @return updated booking DTO
     * @throws BookingNotFoundException    if booking not found
     * @throws BookingAccessDeniedException if the user is not the property host
     */
    @Transactional
    public BookingDto executeByHost(Long bookingId, Long requestingUserId) {
        log.info("HOST {} confirming booking {}", requestingUserId, bookingId);

        // 1. Load booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // 2. Verify that who confirms is the host of the property
        Property property = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(booking.getPropertyId())));

        if (!property.getHostId().equals(requestingUserId)) {
            throw new BookingAccessDeniedException(
                    "Only the property host can confirm booking " + bookingId
            );
        }

        // 3. Snapshot commission rates
        BigDecimal hostCommissionRate = userSubscriptionPort.getHostCommissionRate(requestingUserId);

        BigDecimal affiliateCommissionRate = null;
        if (booking.getReferralLinkId() != null) {
            Long affiliateId = bookingRepository.findAffiliateIdByReferralLinkId(booking.getReferralLinkId())
                    .orElse(null);
            if (affiliateId != null) {
                Optional<Long> organicUserId = bookingRepository.findOrganicSystemUserId();
                boolean isOrganic = organicUserId.isPresent() && organicUserId.get().equals(affiliateId);
                if (isOrganic) {
                    affiliateCommissionRate = BigDecimal.ZERO;
                    hostCommissionRate = new BigDecimal("0.15");
                } else {
                    affiliateCommissionRate = userSubscriptionPort.getAffiliateCommissionRate(affiliateId);
                }
            }
        }

        // 4. Confirm reservation — HOST_CONFIRMED is the system confirmation code when host confirms directly
        Money totalAmount = booking.getTotalAmount() != null
                ? booking.getTotalAmount()
                : Money.of(java.math.BigDecimal.ZERO, "USD");

        booking.reserve("HOST_CONFIRMED_" + bookingId, totalAmount,
                hostCommissionRate, affiliateCommissionRate, requestingUserId);

        Booking updated = bookingRepository.save(booking);

        // 5. Block booking dates in property calendar
        List<PropertyCalendar> blockedDates = PropertyCalendar.blockForBooking(
                booking.getPropertyId(),
                booking.getBookingDates().getCheckInDate(),
                booking.getBookingDates().getCheckOutDate(),
                bookingId,
                requestingUserId
        );
        calendarRepository.saveAll(blockedDates);

        log.info("Booking {} confirmed by HOST {} — {} dates blocked in calendar",
                bookingId, requestingUserId, blockedDates.size());

        return bookingMapper.toDto(updated);
    }

    /**
     * Sends a booking-reserved email to the host. Fire-and-forget: never throws.
     */
    private void sendBookingReservedEmail(Booking booking, Long hostId) {
        try {
            userRepository.findById(hostId).ifPresent(host -> {
                String propertyTitle = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
                        .map(p -> p.getTitle())
                        .orElse("tu propiedad");

                EmailMessage email = emailTemplateService.buildBookingReservedEmail(
                        host.email().address(),
                        new EmailTemplateService.BookingReservedEmailData(
                                host.name(),
                                booking.getGuestInfo().getName(),
                                propertyTitle,
                                booking.getBookingDates().getCheckInDate(),
                                booking.getBookingDates().getCheckOutDate(),
                                booking.getTotalAmount() != null ? booking.getTotalAmount().getAmount() : java.math.BigDecimal.ZERO
                        )
                );
                emailService.sendEmail(email);
                log.info("Booking reserved email sent to host {} for booking {}", hostId, booking.getId());
            });
        } catch (Exception e) {
            log.error("Failed to send booking reserved email for booking {} to host {}: {}",
                    booking.getId(), hostId, e.getMessage());
        }
    }
}
