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

    public CreateBookingUseCaseImpl(BookingRepositoryPort bookingRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public BookingResponse execute(CreateBookingCommand command) {
        log.info("Creating booking request for property {} via referral link {}",
            command.propertyId(), command.referralLinkId());

        // 1. Create domain value objects
        DateRange dateRange = new DateRange(command.startDate(), command.endDate());
        ClientInfo clientInfo = new ClientInfo(
            command.clientEmail(),
            command.clientPhone(),
            command.clientFirstName(),
            command.clientLastName()
        );

        // 2. Check property availability for requested dates
        if (!bookingRepository.isPropertyAvailable(command.propertyId(), dateRange)) {
            log.warn("Property {} not available for dates {} to {}",
                command.propertyId(), command.startDate(), command.endDate());
            throw new DateRangeNotAvailableException(command.startDate(), command.endDate());
        }

        // 3. Create booking (domain logic)
        Booking booking = Booking.createRequest(
            command.referralLinkId(),
            command.propertyId(),
            dateRange,
            clientInfo,
            command.totalAmount(),
            command.currency()
        );

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
            savedBooking.getCreatedAt()
        );
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
            booking.getTotalAmount(),
            booking.getCurrency(),
            booking.getStatus().name(),
            booking.getCreatedAt().format(DATE_TIME_FORMATTER)
        );
    }
}
