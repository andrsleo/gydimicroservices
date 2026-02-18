package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Start a booking (RESERVED → IN_PROGRESS).
 * <p>
 * This transition is normally automated by the {@link com.affiliate.rentals.gydi.bookings.infrastructure.scheduler.BookingStatusScheduler}
 * on the check-in date. This use case provides a manual override for admins
 * when the scheduler has not yet run or an immediate transition is required.
 * </p>
 * <p>
 * <strong>Note:</strong> This endpoint does NOT publish any domain event.
 * Commission creation is only triggered upon FINISHED transition, not IN_PROGRESS.
 * </p>
 */
@Service
public class StartBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;

    public StartBookingUseCase(BookingRepositoryPort bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Executes the manual check-in (RESERVED → IN_PROGRESS).
     *
     * @param bookingId the booking to start
     * @return updated booking DTO
     * @throws BookingNotFoundException if booking not found
     * @throws com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException
     *         if booking is not in RESERVED status
     */
    @Transactional
    public BookingDto execute(Long bookingId) {
        log.info("Starting booking {} manually (admin override)", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        booking.startStay();

        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} started successfully (check-in registered)", bookingId);

        return bookingMapper.toDto(updated);
    }
}
