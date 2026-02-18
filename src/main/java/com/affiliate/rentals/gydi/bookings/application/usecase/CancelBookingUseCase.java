package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.CancelBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Cancel a booking.
 */
@Service
public class CancelBookingUseCase {
    private static final Logger log = LoggerFactory.getLogger(CancelBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;

    public CancelBookingUseCase(BookingRepositoryPort bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingDto execute(Long bookingId, CancelBookingRequest request) {
        log.info("Cancelling booking {} by user {}", bookingId, request.cancelledBy());

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));

        booking.cancel(request.cancelledBy(), request.reason());

        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingId);

        return bookingMapper.toDto(updated);
    }
}
