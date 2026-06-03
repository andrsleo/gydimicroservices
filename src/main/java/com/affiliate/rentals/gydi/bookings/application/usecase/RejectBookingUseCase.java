package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Use Case: HOST rejects a booking request (REQUEST → CANCELLED).
 * <p>
 * Business Flow:
 * 1. Load booking
 * 2. Verify the requesting user is the host of the property
 * 3. Cancel the booking with the host's rejection reason
 * </p>
 */
@Service
public class RejectBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(RejectBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;
    private final PropertyRepositoryPort propertyRepository;

    public RejectBookingUseCase(BookingRepositoryPort bookingRepository,
                                BookingMapper bookingMapper,
                                PropertyRepositoryPort propertyRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.bookingMapper = Objects.requireNonNull(bookingMapper);
        this.propertyRepository = Objects.requireNonNull(propertyRepository);
    }

    @Transactional
    public BookingDto execute(Long bookingId, String reason, Long requestingHostId) {
        log.info("HOST {} rejecting booking {}", requestingHostId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Verify host owns the property
        Property property = propertyRepository.findById(PropertyId.of(booking.getPropertyId()))
                .orElseThrow(() -> new PropertyNotFoundException(String.valueOf(booking.getPropertyId())));

        if (!property.getHostId().equals(requestingHostId)) {
            throw new BookingAccessDeniedException(
                    "Only the property host can reject booking " + bookingId);
        }

        // Cancel the booking with the rejection reason
        booking.cancel(requestingHostId, "HOST_REJECTED: " + reason);
        // Store rejection reason for direct field access in API response
        booking.setRejectionReason(reason);

        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} rejected by HOST {}", bookingId, requestingHostId);

        return bookingMapper.toDto(updated);
    }
}
