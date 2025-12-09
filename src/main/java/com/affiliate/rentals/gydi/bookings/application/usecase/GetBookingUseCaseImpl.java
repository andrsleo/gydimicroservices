package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.port.in.CreateBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.in.GetBookingUseCase;
import com.affiliate.rentals.gydi.bookings.domain.port.out.BookingRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Query bookings.
 */
@Service
@Transactional(readOnly = true)
public class GetBookingUseCaseImpl implements GetBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetBookingUseCaseImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookingRepositoryPort bookingRepository;

    public GetBookingUseCaseImpl(BookingRepositoryPort bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public CreateBookingUseCase.BookingResponse getById(Long bookingId) {
        log.debug("Fetching booking by ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));

        return mapToResponse(booking);
    }

    @Override
    public List<CreateBookingUseCase.BookingResponse> getByReferralLinkId(Long referralLinkId) {
        log.debug("Fetching bookings for referral link: {}", referralLinkId);

        List<Booking> bookings = bookingRepository.findByReferralLinkId(referralLinkId);

        log.debug("Found {} bookings for referral link {}", bookings.size(), referralLinkId);

        return bookings.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<CreateBookingUseCase.BookingResponse> getActiveBookingsByProperty(Long propertyId) {
        log.debug("Fetching active bookings for property: {}", propertyId);

        List<Booking> bookings = bookingRepository.findActiveBookingsByProperty(propertyId);

        log.debug("Found {} active bookings for property {}", bookings.size(), propertyId);

        return bookings.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
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
