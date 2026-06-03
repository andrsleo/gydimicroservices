package com.affiliate.rentals.gydi.bookings.application.usecase;

import com.affiliate.rentals.gydi.bookings.application.dto.BookingDto;
import com.affiliate.rentals.gydi.bookings.application.mapper.BookingMapper;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingAccessDeniedException;
import com.affiliate.rentals.gydi.bookings.domain.exception.BookingNotFoundException;
import com.affiliate.rentals.gydi.bookings.domain.model.Booking;
import com.affiliate.rentals.gydi.bookings.domain.model.BookingStatus;
import com.affiliate.rentals.gydi.bookings.domain.ports.BookingRepositoryPort;
import com.affiliate.rentals.gydi.properties.domain.exception.PropertyNotFoundException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RejectBookingUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RejectBookingUseCase")
class RejectBookingUseCaseTest {

    @Mock private BookingRepositoryPort bookingRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private PropertyRepositoryPort propertyRepository;

    private RejectBookingUseCase useCase;

    private static final Long BOOKING_ID = 999L;
    private static final Long PROPERTY_ID = 1L;
    private static final Long HOST_ID = 2L;
    private static final Long OTHER_HOST_ID = 99L;
    private static final String REJECTION_REASON = "Fechas no disponibles por mantenimiento";

    @BeforeEach
    void setUp() {
        useCase = new RejectBookingUseCase(bookingRepository, bookingMapper, propertyRepository);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("debe cancelar booking REQUEST con razón de rechazo")
        void debe_cancelar_booking_con_razon() {
            Booking booking = mockBookingInRequest();
            Property property = mockPropertyOwnedBy(HOST_ID);
            BookingDto expectedDto = mock(BookingDto.class);

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(PropertyId.of(PROPERTY_ID)))
                    .thenReturn(Optional.of(property));
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.toDto(booking)).thenReturn(expectedDto);

            BookingDto result = useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID);

            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("debe llamar booking.cancel() con la razón prefijada con HOST_REJECTED")
        void debe_llamar_cancel_con_prefijo_HOST_REJECTED() {
            Booking booking = mockBookingInRequest();
            Property property = mockPropertyOwnedBy(HOST_ID);

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(property));
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.toDto(booking)).thenReturn(mock(BookingDto.class));

            useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID);

            verify(booking).cancel(eq(HOST_ID), contains("HOST_REJECTED"));
        }

        @Test
        @DisplayName("debe guardar rejectionReason en campo directo")
        void debe_guardar_rejectionReason_en_campo_directo() {
            Booking booking = mockBookingInRequest();
            Property property = mockPropertyOwnedBy(HOST_ID);

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(property));
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.toDto(booking)).thenReturn(mock(BookingDto.class));

            useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID);

            // setRejectionReason stores clean reason (without HOST_REJECTED prefix)
            verify(booking).setRejectionReason(REJECTION_REASON);
        }

        @Test
        @DisplayName("debe persistir el booking actualizado")
        void debe_persistir_booking_actualizado() {
            Booking booking = mockBookingInRequest();
            Property property = mockPropertyOwnedBy(HOST_ID);

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(property));
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.toDto(booking)).thenReturn(mock(BookingDto.class));

            useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID);

            verify(bookingRepository).save(booking);
        }
    }

    // ─── Booking not found ────────────────────────────────────────────────────

    @Nested
    @DisplayName("booking not found")
    class BookingNotFound {

        @Test
        @DisplayName("debe lanzar BookingNotFoundException si booking no existe")
        void debe_lanzar_excepcion_si_booking_no_existe() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    // ─── Property not found ───────────────────────────────────────────────────

    @Nested
    @DisplayName("property not found")
    class PropertyNotFound {

        @Test
        @DisplayName("debe lanzar PropertyNotFoundException si propiedad no existe")
        void debe_lanzar_excepcion_si_propiedad_no_existe() {
            Booking booking = mockBookingInRequest();
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(BOOKING_ID, REJECTION_REASON, HOST_ID))
                    .isInstanceOf(PropertyNotFoundException.class);
        }
    }

    // ─── Authorization ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("debe lanzar BookingAccessDeniedException si no es el host de la propiedad")
        void debe_lanzar_excepcion_si_no_es_el_host() {
            Booking booking = mockBookingInRequest();
            Property property = mockPropertyOwnedBy(HOST_ID); // owned by HOST_ID, not OTHER_HOST_ID

            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> useCase.execute(BOOKING_ID, REJECTION_REASON, OTHER_HOST_ID))
                    .isInstanceOf(BookingAccessDeniedException.class)
                    .hasMessageContaining("Only the property host");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Booking mockBookingInRequest() {
        Booking booking = mock(Booking.class);
        when(booking.getPropertyId()).thenReturn(PROPERTY_ID);
        // cancel() and setRejectionReason() are void — no stub needed
        return booking;
    }

    private Property mockPropertyOwnedBy(Long hostId) {
        Property property = mock(Property.class);
        when(property.getHostId()).thenReturn(hostId);
        return property;
    }
}
