package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.events.BookingFinishedEvent;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Finish a booking (IN_PROGRESS → FINISHED).
 * <p>
 * This transition is normally automated by the {@link com.affiliate.rentals.gydi.bookings.infrastructure.scheduler.BookingStatusScheduler}
 * on the check-out date. This use case provides a manual override for admins
 * when the scheduler has not yet run or an immediate transition is required.
 * </p>
 * <p>
 * <strong>Critical:</strong> Publishes a {@link BookingFinishedEvent} after persisting
 * the state change. This event is consumed by the commissions bounded context
 * ({@link com.affiliate.rentals.gydi.commissions.application.event.BookingFinishedEventHandler})
 * which creates the HostCommission and AffiliateCommission records and immediately
 * charges the host via Stripe.
 * </p>
 * <p>
 * Commission rates are taken from the SNAPSHOTTED values stored in the booking
 * at reservation time (set during the REQUEST → RESERVED transition by
 * {@link ReserveBookingUseCase}), guaranteeing rate consistency.
 * </p>
 */
@Service
public class FinishBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(FinishBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepositoryPort userRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    public FinishBookingUseCase(
            BookingRepositoryPort bookingRepository,
            BookingMapper bookingMapper,
            ApplicationEventPublisher eventPublisher,
            UserRepositoryPort userRepository,
            PropertyRepositoryPort propertyRepository,
            EmailServicePort emailService,
            EmailTemplateService emailTemplateService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Executes the manual check-out (IN_PROGRESS → FINISHED) and triggers commission creation.
     *
     * @param bookingId the booking to finish
     * @return updated booking DTO
     * @throws BookingNotFoundException if booking not found
     * @throws com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException
     *         if booking is not in IN_PROGRESS status
     * @throws IllegalStateException if the booking has no associated host
     */
    @Transactional
    public BookingDto execute(Long bookingId) {
        log.info("Finishing booking {} manually (admin override)", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Transition domain state (IN_PROGRESS → FINISHED)
        booking.finish();
        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} status updated to FINISHED. Preparing commission event.", bookingId);

        // Resolve host from property ownership
        Long hostId = bookingRepository.findHostIdByPropertyId(booking.getPropertyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Host not found for property " + booking.getPropertyId()
                                + " (booking " + bookingId + ")"));

        // Resolve affiliate from referral link (nullable - direct bookings have no affiliate)
        Long affiliateId = null;
        if (booking.getReferralLinkId() != null) {
            affiliateId = bookingRepository.findAffiliateIdByReferralLinkId(booking.getReferralLinkId())
                    .orElse(null);
        }

        // Publish BookingFinishedEvent to trigger commission creation and host charge
        // Event is handled AFTER_COMMIT by BookingFinishedEventHandler in commissions context
        BookingFinishedEvent event = new BookingFinishedEvent(
                updated.getId(),
                updated.getPropertyId(),
                hostId,
                affiliateId,
                updated.getTotalAmount().getAmount(),
                updated.getTotalAmount().getCurrency(),
                updated.getHostCommissionRate(),       // Snapshotted rate from RESERVED transition
                updated.getAffiliateCommissionRate(),  // Snapshotted rate from RESERVED transition (nullable)
                updated.getFinishedAt());

        eventPublisher.publishEvent(event);

        log.info("Booking {} finished successfully. BookingFinishedEvent published for commission processing.",
                bookingId);

        // Fire-and-forget: send booking finished email to host
        sendBookingFinishedEmail(updated, hostId);

        return bookingMapper.toDto(updated);
    }

    /**
     * Sends a booking-finished email to the host. Fire-and-forget: never throws.
     */
    private void sendBookingFinishedEmail(Booking booking, Long hostId) {
        try {
            userRepository.findById(hostId).ifPresent(host -> {
                String propertyTitle = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
                        .map(p -> p.getTitle())
                        .orElse("tu propiedad");

                EmailMessage email = emailTemplateService.buildBookingFinishedEmail(
                        host.email().address(),
                        new EmailTemplateService.BookingFinishedEmailData(
                                host.name(),
                                booking.getGuestInfo().getName(),
                                propertyTitle,
                                booking.getBookingDates().getCheckInDate(),
                                booking.getBookingDates().getCheckOutDate()
                        )
                );
                emailService.sendEmail(email);
                log.info("Booking finished email sent to host {} for booking {}", hostId, booking.getId());
            });
        } catch (Exception e) {
            log.error("Failed to send booking finished email for booking {} to host {}: {}",
                    booking.getId(), hostId, e.getMessage());
        }
    }
}
