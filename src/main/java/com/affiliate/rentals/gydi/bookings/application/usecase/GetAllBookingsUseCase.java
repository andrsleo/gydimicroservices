package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Get all bookings (ADMIN only).
 * <p>
 * Returns all bookings in the system for administrative management.
 * </p>
 */
@Service
public class GetAllBookingsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final BookingMapper bookingMapper;

    public GetAllBookingsUseCase(
        BookingRepositoryPort bookingRepository,
        BookingMapper bookingMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Executes the use case to get all bookings.
     *
     * @return list of all bookings
     */
    @Transactional(readOnly = true)
    public List<BookingDto> execute() {
        List<Booking> bookings = bookingRepository.findAll();

        return bookings.stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }
}
