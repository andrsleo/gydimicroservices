package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Get bookings for a host.
 * <p>
 * Returns all bookings for properties owned by the host.
 * </p>
 */
@Service
public class GetHostBookingsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final BookingMapper bookingMapper;

    public GetHostBookingsUseCase(
        BookingRepositoryPort bookingRepository,
        PropertyRepositoryPort propertyRepository,
        BookingMapper bookingMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Executes the use case to get all bookings for a host.
     *
     * @param hostId the host's user ID
     * @return list of bookings for properties owned by the host
     */
    @Transactional(readOnly = true)
    public List<BookingDto> execute(Long hostId) {
        // Get all properties owned by the host
        List<Property> properties = propertyRepository.findByHostId(hostId);

        // Get all property IDs
        List<Long> propertyIds = properties.stream()
            .map(property -> property.getId().getValue())
            .collect(Collectors.toList());

        // If host has no properties, return empty list
        if (propertyIds.isEmpty()) {
            return List.of();
        }

        // Get all bookings for those properties
        List<Booking> bookings = propertyIds.stream()
            .flatMap(propertyId -> bookingRepository.findByPropertyId(propertyId).stream())
            .collect(Collectors.toList());

        // Convert to DTOs
        return bookings.stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }
}
