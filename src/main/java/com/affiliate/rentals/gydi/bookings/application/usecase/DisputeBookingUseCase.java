package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.DisputeBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Dispute a finished booking (FINISHED → DISPUTED).
 * <p>
 * Used when there is a complaint or issue after the stay is completed.
 * Only FINISHED bookings can be disputed — see {@link com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus}.
 * </p>
 * <p>
 * A dispute allows for commission adjustment or refund processing.
 * The dispute reason is recorded in the status history for audit purposes.
 * </p>
 */
@Service
public class DisputeBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(DisputeBookingUseCase.class);

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;

    public DisputeBookingUseCase(BookingRepositoryPort bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Executes the dispute (FINISHED → DISPUTED).
     *
     * @param bookingId the booking to dispute
     * @param request   dispute details including the user raising the dispute and the reason
     * @return updated booking DTO
     * @throws BookingNotFoundException if booking not found
     * @throws com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException
     *         if booking is not in FINISHED status
     */
    @Transactional
    public BookingDto execute(Long bookingId, DisputeBookingRequest request) {
        log.info("Disputing booking {} by user {} with reason: {}",
                bookingId, request.disputedBy(), request.reason());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        booking.dispute(request.disputedBy(), request.reason());

        Booking updated = bookingRepository.save(booking);

        log.info("Booking {} marked as DISPUTED successfully", bookingId);

        return bookingMapper.toDto(updated);
    }
}
