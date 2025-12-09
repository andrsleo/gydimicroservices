package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.event.BookingCanceledEvent;
import com.affiliate.rentals.gydi.bookings.domain.event.BookingFinishedEvent;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.port.in.CreateBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.in.UpdateBookingStatusUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.out.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Use Case: Update booking status.
 * <p>
 * Handles transitions:
 * - REQUEST → RESERVED (host confirms booking)
 * - RESERVED → FINISHED (stay completed)
 * - REQUEST/RESERVED → CANCELED (booking canceled)
 * </p>
 * <p>
 * <strong>IMPORTANT:</strong> When transitioning to FINISHED,
 * publishes BookingFinishedEvent which triggers payment creation.
 * </p>
 */
@Service
@Transactional
public class UpdateBookingStatusUseCaseImpl implements UpdateBookingStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateBookingStatusUseCaseImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookingRepositoryPort bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateBookingStatusUseCaseImpl(BookingRepositoryPort bookingRepository,
                                           ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CreateBookingUseCase.BookingResponse execute(UpdateStatusCommand command) {
        log.info("Updating booking {} status to {}", command.bookingId(), command.targetStatus());

        // 1. Load booking
        Booking booking = bookingRepository.findById(command.bookingId())
            .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        BookingStatus currentStatus = booking.getStatus();

        // 2. Execute state transition (domain logic handles validation)
        switch (command.targetStatus()) {
            case "RESERVED" -> {
                booking.reserve();
                log.info("Booking {} reserved successfully", booking.getId());
            }
            case "FINISHED" -> {
                booking.finish();
                log.info("Booking {} finished successfully", booking.getId());

                // Publish event to trigger payment creation
                publishBookingFinishedEvent(booking);
            }
            case "CANCELED" -> {
                booking.cancel();
                log.info("Booking {} canceled", booking.getId());

                // Publish cancellation event
                publishBookingCanceledEvent(booking);
            }
            default -> throw new IllegalArgumentException(
                "Invalid target status: " + command.targetStatus()
            );
        }

        // 3. Persist state change
        Booking updatedBooking = bookingRepository.save(booking);

        log.info("Booking {} status changed: {} → {}",
            updatedBooking.getId(), currentStatus, updatedBooking.getStatus());

        // 4. Map to response
        return mapToResponse(updatedBooking);
    }

    /**
     * Publishes BookingFinishedEvent.
     * <p>
     * This event is consumed by PaymentEventHandler to create payment.booking record.
     * Financial details (amount, currency) will be determined by the payment/commission system.
     * </p>
     */
    private void publishBookingFinishedEvent(Booking booking) {
        // NOTE: userId comes from referral_link, we need to fetch it
        // For now, we'll set it to null and handle it in the event handler
        // TODO: Add ReferralLinkRepositoryPort to fetch userId

        BookingFinishedEvent event = new BookingFinishedEvent(
            booking.getId(),
            booking.getReferralLinkId(),
            booking.getPropertyId(),
            null, // TODO: Fetch from referral link
            booking.getDateRange().getStartDate(),
            booking.getDateRange().getEndDate(),
            booking.getUpdatedAt()
        );

        eventPublisher.publishEvent(event);
        log.debug("BookingFinishedEvent published for booking {}", booking.getId());
    }

    /**
     * Publishes BookingCanceledEvent.
     * <p>
     * Cancellation details (reason, who canceled, timestamp) are not tracked in the booking table.
     * The event only contains basic cancellation information.
     * </p>
     */
    private void publishBookingCanceledEvent(Booking booking) {
        BookingCanceledEvent event = new BookingCanceledEvent(
            booking.getId(),
            booking.getPropertyId(),
            booking.getUpdatedAt()
        );

        eventPublisher.publishEvent(event);
        log.debug("BookingCanceledEvent published for booking {}", booking.getId());
    }

    private CreateBookingUseCase.BookingResponse mapToResponse(Booking booking) {
        return new CreateBookingUseCase.BookingResponse(
            booking.getId(),
            booking.getReferralLinkId(),
            booking.getPropertyId(),
            booking.getDateRange().getStartDate(),
            booking.getDateRange().getEndDate(),
            booking.getClientInfo().getEmail(),
            booking.getClientInfo().fullName(),
            booking.getStatus().name(),
            booking.getCreatedAt().format(DATE_TIME_FORMATTER)
        );
    }
}
