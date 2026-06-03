package com.affiliate.rentals.gydi.bookings.domain.model;

import com.affiliate.rentals.gydi.bookings.domain.exception.InvalidBookingStatusTransitionException;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.BookingDates;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.GuestInfo;
import com.affiliate.rentals.gydi.bookings.domain.model.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Booking aggregate root.
 *
 * Tests domain business logic in isolation — no Spring, no JPA, no Mockito.
 */
@DisplayName("Booking Domain Model")
class BookingDomainModelTest {

    private static final Long REFERRAL_LINK_ID = 42L;
    private static final Long PROPERTY_ID = 1L;
    private static final Long HOST_ID = 2L;
    private static final Long ADMIN_ID = 999L;

    private BookingDates futureDates;
    private GuestInfo guestInfo;

    @BeforeEach
    void setUp() {
        futureDates = BookingDates.of(
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(13));
        guestInfo = GuestInfo.of("Juan Pérez García", "juan@test.com", "+573001234567", 2);
    }

    // ─── Factory: createRequest ───────────────────────────────────────────────

    @Nested
    @DisplayName("createRequest()")
    class CreateRequestTests {

        @Test
        @DisplayName("debe crear booking en status REQUEST")
        void debe_crearBooking_en_status_REQUEST() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUEST);
        }

        @Test
        @DisplayName("debe inicializar sin ID (asignado por persistencia)")
        void debe_inicializar_sin_id() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThat(booking.getId()).isNull();
        }

        @Test
        @DisplayName("debe guardar referralLinkId, propertyId y guestInfo")
        void debe_guardar_campos_requeridos() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThat(booking.getReferralLinkId()).isEqualTo(REFERRAL_LINK_ID);
            assertThat(booking.getPropertyId()).isEqualTo(PROPERTY_ID);
            assertThat(booking.getGuestInfo().getName()).isEqualTo("Juan Pérez García");
            assertThat(booking.getGuestInfo().getEmail()).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("debe rechazar referralLinkId null")
        void debe_rechazar_referralLinkId_null() {
            assertThatThrownBy(() -> Booking.createRequest(null, PROPERTY_ID, futureDates, guestInfo))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Referral link ID");
        }

        @Test
        @DisplayName("debe rechazar propertyId null")
        void debe_rechazar_propertyId_null() {
            assertThatThrownBy(() -> Booking.createRequest(REFERRAL_LINK_ID, null, futureDates, guestInfo))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Property ID");
        }

        @Test
        @DisplayName("debe inicializar totalAmount y commissionRates como null")
        void debe_inicializar_amounts_como_null() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThat(booking.getTotalAmount()).isNull();
            assertThat(booking.getHostCommissionRate()).isNull();
            assertThat(booking.getAffiliateCommissionRate()).isNull();
        }

        @Test
        @DisplayName("debe inicializar Stripe fields como null")
        void debe_inicializar_stripe_fields_como_null() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThat(booking.getStripeBookingIntentId()).isNull();
            assertThat(booking.getStripeDepositIntentId()).isNull();
            assertThat(booking.getDepositAmount()).isNull();
        }
    }

    // ─── addInitialStatusHistory ──────────────────────────────────────────────

    @Nested
    @DisplayName("addInitialStatusHistory()")
    class AddInitialStatusHistoryTests {

        @Test
        @DisplayName("debe agregar entry de historial tras asignar ID")
        void debe_agregar_historial_tras_asignar_id() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);
            booking.setId(1L);

            booking.addInitialStatusHistory();

            assertThat(booking.getStatusHistory()).hasSize(1);
            assertThat(booking.getStatusHistory().get(0).getNewStatus()).isEqualTo(BookingStatus.REQUEST);
        }

        @Test
        @DisplayName("debe lanzar IllegalStateException si ID es null")
        void debe_lanzar_excepcion_si_id_es_null() {
            Booking booking = Booking.createRequest(REFERRAL_LINK_ID, PROPERTY_ID, futureDates, guestInfo);

            assertThatThrownBy(booking::addInitialStatusHistory)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot add initial status history before booking is persisted");
        }
    }

    // ─── cancel() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("debe transicionar REQUEST → CANCELLED")
        void debe_transicionar_REQUEST_a_CANCELLED() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            booking.cancel(HOST_ID, "Host rejected the request");

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("debe agregar historial de cancelación")
        void debe_agregar_historial_cancelacion() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            booking.cancel(HOST_ID, "Test reason");

            assertThat(booking.getStatusHistory()).hasSize(1);
            assertThat(booking.getStatusHistory().get(0).getNewStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("no debe cancelar booking FINISHED")
        void no_debe_cancelar_booking_FINISHED() {
            Booking booking = reconstituted(BookingStatus.FINISHED);

            assertThatThrownBy(() -> booking.cancel(HOST_ID, "reason"))
                    .isInstanceOf(InvalidBookingStatusTransitionException.class);
        }

        @Test
        @DisplayName("no debe cancelar booking ya CANCELLED")
        void no_debe_cancelar_booking_ya_CANCELLED() {
            Booking booking = reconstituted(BookingStatus.CANCELLED);

            assertThatThrownBy(() -> booking.cancel(HOST_ID, "reason"))
                    .isInstanceOf(InvalidBookingStatusTransitionException.class);
        }

        @Test
        @DisplayName("debe requerir cancelledBy no null")
        void debe_requerir_cancelledBy_no_null() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            assertThatThrownBy(() -> booking.cancel(null, "reason"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ─── setRejectionReason() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("setRejectionReason()")
    class SetRejectionReasonTests {

        @Test
        @DisplayName("debe guardar la razón de rechazo")
        void debe_guardar_razon_de_rechazo() {
            Booking booking = reconstituted(BookingStatus.REQUEST);
            booking.cancel(HOST_ID, "HOST_REJECTED: Fechas bloqueadas");

            booking.setRejectionReason("Fechas bloqueadas");

            assertThat(booking.getRejectionReason()).isEqualTo("Fechas bloqueadas");
        }
    }

    // ─── Stripe Phase 2 setters ────────────────────────────────────────────────

    @Nested
    @DisplayName("Stripe Phase 2 fields")
    class StripeFieldsTests {

        @Test
        @DisplayName("debe guardar stripeBookingIntentId y actualizar updatedAt")
        void debe_guardar_stripeBookingIntentId() {
            Booking booking = reconstituted(BookingStatus.REQUEST);
            LocalDateTime before = booking.getUpdatedAt();

            booking.setStripeBookingIntentId("pi_test_booking_123");

            assertThat(booking.getStripeBookingIntentId()).isEqualTo("pi_test_booking_123");
            assertThat(booking.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("debe guardar stripeDepositIntentId")
        void debe_guardar_stripeDepositIntentId() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            booking.setStripeDepositIntentId("pi_test_deposit_456");

            assertThat(booking.getStripeDepositIntentId()).isEqualTo("pi_test_deposit_456");
        }

        @Test
        @DisplayName("debe guardar depositAmount")
        void debe_guardar_depositAmount() {
            Booking booking = reconstituted(BookingStatus.REQUEST);
            Money deposit = Money.of(BigDecimal.valueOf(150), "USD");

            booking.setDepositAmount(deposit);

            assertThat(booking.getDepositAmount()).isEqualTo(deposit);
        }

        @Test
        @DisplayName("debe registrar captura de depósito con monto parcial")
        void debe_registrar_captura_deposito_parcial() {
            Booking booking = reconstituted(BookingStatus.FINISHED);
            booking.setDepositAmount(Money.of(BigDecimal.valueOf(150), "USD"));

            booking.recordDepositCapture(5000L, "Daños menores"); // $50.00

            assertThat(booking.getDepositCapturedAt()).isNotNull();
            assertThat(booking.getDepositCaptureAmount()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("recordDepositCapture sin monto usa totalDepositAmount")
        void recordDepositCapture_sin_monto_usa_total() {
            Booking booking = reconstituted(BookingStatus.FINISHED);
            booking.setDepositAmount(Money.of(BigDecimal.valueOf(150), "USD"));

            booking.recordDepositCapture(null, "Full capture");

            assertThat(booking.getDepositCaptureAmount()).isEqualByComparingTo("150.00");
        }
    }

    // ─── startStay() / finish() ───────────────────────────────────────────────

    @Nested
    @DisplayName("State transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("startStay: RESERVED → IN_PROGRESS")
        void startStay_transiciona_RESERVED_a_IN_PROGRESS() {
            Booking booking = reconstituted(BookingStatus.RESERVED);

            booking.startStay();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
            assertThat(booking.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("finish: IN_PROGRESS → FINISHED")
        void finish_transiciona_IN_PROGRESS_a_FINISHED() {
            Booking booking = reconstituted(BookingStatus.IN_PROGRESS);

            booking.finish();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.FINISHED);
            assertThat(booking.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("dispute: FINISHED → DISPUTED")
        void dispute_transiciona_FINISHED_a_DISPUTED() {
            Booking booking = reconstituted(BookingStatus.FINISHED);

            booking.dispute(HOST_ID, "Property damaged");

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.DISPUTED);
        }

        @Test
        @DisplayName("startStay falla desde REQUEST")
        void startStay_falla_desde_REQUEST() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            assertThatThrownBy(booking::startStay)
                    .isInstanceOf(InvalidBookingStatusTransitionException.class);
        }

        @Test
        @DisplayName("finish falla desde REQUEST")
        void finish_falla_desde_REQUEST() {
            Booking booking = reconstituted(BookingStatus.REQUEST);

            assertThatThrownBy(booking::finish)
                    .isInstanceOf(InvalidBookingStatusTransitionException.class);
        }
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Query methods")
    class QueryMethodTests {

        @Test
        @DisplayName("isModifiable es true para REQUEST")
        void isModifiable_true_para_REQUEST() {
            assertThat(reconstituted(BookingStatus.REQUEST).isModifiable()).isTrue();
        }

        @Test
        @DisplayName("isModifiable es false para FINISHED")
        void isModifiable_false_para_FINISHED() {
            assertThat(reconstituted(BookingStatus.FINISHED).isModifiable()).isFalse();
        }

        @Test
        @DisplayName("isCancelled es true para CANCELLED")
        void isCancelled_true_para_CANCELLED() {
            assertThat(reconstituted(BookingStatus.CANCELLED).isCancelled()).isTrue();
        }

        @Test
        @DisplayName("isCompleted es true para FINISHED")
        void isCompleted_true_para_FINISHED() {
            assertThat(reconstituted(BookingStatus.FINISHED).isCompleted()).isTrue();
        }
    }

    // ─── BookingDates VO ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("BookingDates VO")
    class BookingDatesTests {

        @Test
        @DisplayName("debe rechazar check-in en el pasado")
        void debe_rechazar_checkIn_en_pasado() {
            assertThatThrownBy(() -> BookingDates.of(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be in the future");
        }

        @Test
        @DisplayName("debe rechazar check-in hoy")
        void debe_rechazar_checkIn_hoy() {
            assertThatThrownBy(() -> BookingDates.of(
                    LocalDate.now(),
                    LocalDate.now().plusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("debe rechazar check-out igual a check-in")
        void debe_rechazar_checkOut_igual_checkIn() {
            LocalDate future = LocalDate.now().plusDays(10);
            assertThatThrownBy(() -> BookingDates.of(future, future))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("debe rechazar estancia mayor a 30 días")
        void debe_rechazar_estancia_mayor_30_dias() {
            assertThatThrownBy(() -> BookingDates.of(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(32)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maximum allowed stay");
        }

        @Test
        @DisplayName("reconstitute permite fechas pasadas")
        void reconstitute_permite_fechas_pasadas() {
            BookingDates dates = BookingDates.reconstitute(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 5));

            assertThat(dates.getNumberOfNights()).isEqualTo(4);
        }
    }

    // ─── GuestInfo VO ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GuestInfo VO")
    class GuestInfoTests {

        @Test
        @DisplayName("debe rechazar email inválido")
        void debe_rechazar_email_invalido() {
            assertThatThrownBy(() -> GuestInfo.of("Juan", "no-es-email", null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("debe rechazar nombre vacío")
        void debe_rechazar_nombre_vacio() {
            assertThatThrownBy(() -> GuestInfo.of("", "juan@test.com", null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("debe rechazar guestsCount = 0")
        void debe_rechazar_guestsCount_cero() {
            assertThatThrownBy(() -> GuestInfo.of("Juan", "juan@test.com", null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 50");
        }

        @Test
        @DisplayName("debe normalizar email a minúsculas")
        void debe_normalizar_email_a_minusculas() {
            GuestInfo info = GuestInfo.of("Juan", "JUAN@TEST.COM", null, 1);

            assertThat(info.getEmail()).isEqualTo("juan@test.com");
        }
    }

    // ─── Test helpers ─────────────────────────────────────────────────────────

    /**
     * Reconstitutes a booking in the given status using past dates
     * (to avoid future-date validation on BookingDates.of()).
     */
    private Booking reconstituted(BookingStatus status) {
        return Booking.reconstitute(
                1L,
                REFERRAL_LINK_ID,
                PROPERTY_ID,
                BookingDates.reconstitute(
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 13)),
                GuestInfo.of("Juan Pérez García", "juan@test.com", "+573001234567", 2),
                null,
                status == BookingStatus.RESERVED || status == BookingStatus.IN_PROGRESS || status == BookingStatus.FINISHED
                        ? Money.of(BigDecimal.valueOf(300), "USD") : null,
                status == BookingStatus.RESERVED || status == BookingStatus.IN_PROGRESS || status == BookingStatus.FINISHED
                        ? BigDecimal.valueOf(0.15) : null,
                null,
                status,
                new ArrayList<>(),
                status == BookingStatus.RESERVED || status == BookingStatus.IN_PROGRESS || status == BookingStatus.FINISHED
                        ? LocalDateTime.now().minusDays(1) : null,
                status == BookingStatus.IN_PROGRESS || status == BookingStatus.FINISHED
                        ? LocalDateTime.now().minusDays(1) : null,
                status == BookingStatus.FINISHED ? LocalDateTime.now() : null,
                status == BookingStatus.CANCELLED ? LocalDateTime.now() : null,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(5));
    }
}
