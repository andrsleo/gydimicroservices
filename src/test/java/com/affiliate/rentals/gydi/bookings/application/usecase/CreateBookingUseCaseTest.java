package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.dto.CreateBookingRequest;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.DateRangeNotAvailableException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.bookings.domain.ports.PropertyCalendarRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateBookingUseCase.
 *
 * Tests the use case orchestration logic with all ports mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBookingUseCase")
class CreateBookingUseCaseTest {

    @Mock private BookingRepositoryPort bookingRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private PropertyRepositoryPort propertyRepository;
    @Mock private PropertyCalendarRepositoryPort calendarRepository;
    @Mock private ContentPostRepositoryPort contentPostRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateBookingUseCase useCase;

    private static final Long REFERRAL_LINK_ID = 42L;
    private static final Long PROPERTY_ID = 1L;

    private CreateBookingRequest validRequest;

    @BeforeEach
    void setUp() {
        useCase = new CreateBookingUseCase(bookingRepository, bookingMapper, propertyRepository, calendarRepository, contentPostRepository, eventPublisher);

        validRequest = new CreateBookingRequest(
                REFERRAL_LINK_ID,
                PROPERTY_ID,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(13),
                "Juan Pérez García",
                "juan@test.com",
                "+573001234567",
                2,
                null);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("debe crear booking y retornar DTO cuando propiedad publicada y fechas libres")
        void debe_crear_booking_retornar_dto() {
            // Arrange
            Property publishedProperty = mockPublishedProperty();
            Booking savedBooking = mockSavedBooking();
            BookingDto expectedDto = mock(BookingDto.class);

            when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
                    .thenReturn(Optional.of(publishedProperty));
            when(calendarRepository.hasAnyBlockedDate(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(false);
            when(bookingRepository.existsOverlappingBooking(eq(PROPERTY_ID), any(), any()))
                    .thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
            when(bookingMapper.toDto(savedBooking)).thenReturn(expectedDto);

            // Act
            BookingDto result = useCase.execute(validRequest);

            // Assert
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("debe guardar booking con status REQUEST")
        void debe_guardar_booking_en_status_REQUEST() {
            // Arrange
            Property publishedProperty = mockPublishedProperty();
            Booking savedBooking = mockSavedBooking();

            when(propertyRepository.findById(any())).thenReturn(Optional.of(publishedProperty));
            when(calendarRepository.hasAnyBlockedDate(any(), any(), any())).thenReturn(false);
            when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any())).thenReturn(savedBooking);
            when(bookingMapper.toDto(any(Booking.class))).thenReturn(mock(BookingDto.class));

            // Act
            useCase.execute(validRequest);

            // Assert: first save creates booking in REQUEST status
            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository, atLeastOnce()).save(captor.capture());
            Booking captured = captor.getAllValues().get(0);
            assertThat(captured.getStatus()).isEqualTo(BookingStatus.REQUEST);
        }

        @Test
        @DisplayName("debe guardar booking dos veces (booking + status history)")
        void debe_guardar_booking_dos_veces() {
            Property publishedProperty = mockPublishedProperty();
            Booking savedBooking = mockSavedBooking();

            when(propertyRepository.findById(any())).thenReturn(Optional.of(publishedProperty));
            when(calendarRepository.hasAnyBlockedDate(any(), any(), any())).thenReturn(false);
            when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any())).thenReturn(savedBooking);
            when(bookingMapper.toDto(any(Booking.class))).thenReturn(mock(BookingDto.class));

            useCase.execute(validRequest);

            // Two saves: 1st assigns ID, 2nd persists status history
            verify(bookingRepository, times(2)).save(any(Booking.class));
        }

        @Test
        @DisplayName("debe verificar disponibilidad del calendario")
        void debe_verificar_disponibilidad_calendario() {
            Property publishedProperty = mockPublishedProperty();
            Booking savedBooking = mockSavedBooking();

            when(propertyRepository.findById(any())).thenReturn(Optional.of(publishedProperty));
            when(calendarRepository.hasAnyBlockedDate(any(), any(), any())).thenReturn(false);
            when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
            when(bookingRepository.save(any())).thenReturn(savedBooking);
            when(bookingMapper.toDto(any(Booking.class))).thenReturn(mock(BookingDto.class));

            useCase.execute(validRequest);

            verify(calendarRepository).hasAnyBlockedDate(
                    eq(PROPERTY_ID),
                    eq(validRequest.checkInDate()),
                    eq(validRequest.checkOutDate()));
        }
    }

    // ─── Property validation ──────────────────────────────────────────────────

    @Nested
    @DisplayName("property validation")
    class PropertyValidation {

        @Test
        @DisplayName("debe lanzar PropertyNotFoundException si propiedad no existe")
        void debe_lanzar_excepcion_si_propiedad_no_existe() {
            when(propertyRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(validRequest))
                    .isInstanceOf(PropertyNotFoundException.class);
        }

        @Test
        @DisplayName("debe lanzar PropertyNotAvailableException si propiedad no está PUBLISHED")
        void debe_lanzar_excepcion_si_propiedad_no_publicada() {
            Property draftProperty = mock(Property.class);
            when(draftProperty.getStatus()).thenReturn(PropertyStatus.DRAFT);
            when(propertyRepository.findById(any())).thenReturn(Optional.of(draftProperty));

            assertThatThrownBy(() -> useCase.execute(validRequest))
                    .hasMessageContaining("not available for booking");
        }
    }

    // ─── Date conflict validation ─────────────────────────────────────────────

    @Nested
    @DisplayName("date conflict validation")
    class DateConflictValidation {

        @Test
        @DisplayName("debe lanzar DateRangeNotAvailableException si fechas bloqueadas en calendario")
        void debe_lanzar_excepcion_si_fechas_bloqueadas() {
            Property publishedProperty = mockPublishedProperty();
            when(propertyRepository.findById(any())).thenReturn(Optional.of(publishedProperty));
            when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
            when(calendarRepository.hasAnyBlockedDate(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(validRequest))
                    .isInstanceOf(DateRangeNotAvailableException.class);
        }

        @Test
        @DisplayName("debe lanzar PropertyNotAvailableException si existe booking solapado")
        void debe_lanzar_excepcion_si_booking_solapado() {
            Property publishedProperty = mockPublishedProperty();
            when(propertyRepository.findById(any())).thenReturn(Optional.of(publishedProperty));
            when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(validRequest))
                    .hasMessageContaining("conflicting booking");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Property mockPublishedProperty() {
        Property property = mock(Property.class);
        when(property.getStatus()).thenReturn(PropertyStatus.PUBLISHED);
        return property;
    }

    private Booking mockSavedBooking() {
        Booking booking = mock(Booking.class);
        lenient().when(booking.getId()).thenReturn(999L);
        lenient().when(booking.getStatus()).thenReturn(BookingStatus.REQUEST);
        return booking;
    }
}
