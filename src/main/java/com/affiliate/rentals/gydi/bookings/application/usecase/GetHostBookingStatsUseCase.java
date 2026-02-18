package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingStatsDto;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use Case: Get booking statistics for a host.
 * <p>
 * Calculates total bookings, active bookings, and total revenue
 * from bookings on properties owned by the host.
 * </p>
 */
@Service
public class GetHostBookingStatsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final PropertyRepositoryPort propertyRepository;

    public GetHostBookingStatsUseCase(
        BookingRepositoryPort bookingRepository,
        PropertyRepositoryPort propertyRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
    }

    /**
     * Executes the use case to get booking statistics for a host.
     *
     * @param hostId the host's user ID
     * @return booking statistics
     */
    @Transactional(readOnly = true)
    public BookingStatsDto execute(Long hostId) {
        // Get all properties owned by the host
        List<Property> properties = propertyRepository.findByHostId(hostId);

        // Get all property IDs
        List<Long> propertyIds = properties.stream()
            .map(property -> property.getId().getValue())
            .collect(Collectors.toList());

        // If host has no properties, return empty stats
        if (propertyIds.isEmpty()) {
            return new BookingStatsDto(0, 0, BigDecimal.ZERO, "USD");
        }

        // Get all bookings for those properties
        List<Booking> bookings = propertyIds.stream()
            .flatMap(propertyId -> bookingRepository.findByPropertyId(propertyId).stream())
            .collect(Collectors.toList());

        // Calculate stats
        int totalBookings = bookings.size();

        // Active bookings: RESERVED, IN_PROGRESS
        int activeBookings = (int) bookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.RESERVED ||
                             booking.getStatus() == BookingStatus.IN_PROGRESS)
            .count();

        // Total revenue: sum of all FINISHED bookings
        // Filter bookings that have totalAmount set (not null)
        BigDecimal totalRevenue = bookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.FINISHED)
            .map(Booking::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce((money1, money2) -> money1.add(money2))
            .map(money -> money.getAmount())
            .orElse(BigDecimal.ZERO);

        // Get currency from first booking with amount, default to USD
        String currency = bookings.stream()
            .map(Booking::getTotalAmount)
            .filter(Objects::nonNull)
            .findFirst()
            .map(money -> money.getCurrency())
            .orElse("USD");

        return new BookingStatsDto(totalBookings, activeBookings, totalRevenue, currency);
    }
}
