package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.ReserveBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.commissions.domain.ports.UserSubscriptionPort;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveBookingUseCaseTest {

    @Mock
    private BookingRepositoryPort bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private UserSubscriptionPort userSubscriptionPort;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PropertyRepositoryPort propertyRepository;
    @Mock
    private EmailServicePort emailService;
    @Mock
    private EmailTemplateService emailTemplateService;

    private ReserveBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReserveBookingUseCase(
                bookingRepository, bookingMapper, userSubscriptionPort,
                userRepository, propertyRepository, emailService, emailTemplateService);
    }

    @Test
    void shouldReserveBookingWithCommissionSnapshot() {
        // Given
        Long bookingId = 1L;
        Long propertyId = 100L;
        Long hostId = 200L;
        Long adminId = 999L;
        String airbnbCode = "ABC12345";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        BigDecimal hostRate = new BigDecimal("0.10");

        ReserveBookingRequest request = new ReserveBookingRequest(
                airbnbCode,
                amount,
                currency,
                adminId,
                null, // checkInDate
                null // checkOutDate
        );

        Booking booking = mock(Booking.class);
        when(booking.getPropertyId()).thenReturn(propertyId);
        when(booking.getReferralLinkId()).thenReturn(null); // No affiliate

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findHostIdByPropertyId(propertyId)).thenReturn(Optional.of(hostId));
        when(userSubscriptionPort.getHostCommissionRate(hostId)).thenReturn(hostRate);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(mock(BookingDto.class));

        // When
        BookingDto result = useCase.execute(bookingId, request);

        // Then
        assertNotNull(result);

        // Verify rate fetching
        verify(userSubscriptionPort).getHostCommissionRate(hostId);

        // Verify reserve call includes snapshot rate
        verify(booking).reserve(
                eq(airbnbCode),
                any(), // Money object
                eq(hostRate),
                eq(null), // affiliate rate
                eq(adminId));

        verify(bookingRepository).save(booking);
    }
}
