package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.event.AgreementCancelledEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelAgreementUseCase")
class CancelAgreementUseCaseTest {

    @Mock private AgreementRepositoryPort agreementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CancelAgreementUseCase useCase;

    private static final Long AGREEMENT_ID = 50L;
    private static final Long HOST_ID      = 10L;
    private static final Long CREATOR_ID   = 20L;

    @BeforeEach
    void setUp() {
        useCase = new CancelAgreementUseCase(agreementRepository, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host cancels ACTIVE agreement → CANCELLED, event published with cancelledBy=HOST")
        void host_cancels_active_agreement() {
            CollaborationAgreement agreement = activeAgreement();
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            useCase.execute(AGREEMENT_ID, HOST_ID);

            assertThat(agreement.status()).isEqualTo(AgreementStatus.CANCELLED);
            assertThat(agreement.cancelledBy()).isEqualTo(OfferedBy.HOST);

            ArgumentCaptor<AgreementCancelledEvent> captor =
                    ArgumentCaptor.forClass(AgreementCancelledEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().cancelledBy()).isEqualTo("HOST");
        }

        @Test
        @DisplayName("creator cancels ACTIVE agreement → CANCELLED, cancelledBy=CREATOR")
        void creator_cancels_active_agreement() {
            CollaborationAgreement agreement = activeAgreement();
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            useCase.execute(AGREEMENT_ID, CREATOR_ID);

            assertThat(agreement.status()).isEqualTo(AgreementStatus.CANCELLED);
            assertThat(agreement.cancelledBy()).isEqualTo(OfferedBy.CREATOR);

            ArgumentCaptor<AgreementCancelledEvent> captor =
                    ArgumentCaptor.forClass(AgreementCancelledEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().cancelledBy()).isEqualTo("CREATOR");
        }

        @Test
        @DisplayName("published event carries correct pitchId, hostId, creatorId")
        void event_carries_correct_ids() {
            CollaborationAgreement agreement = activeAgreement();
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            useCase.execute(AGREEMENT_ID, HOST_ID);

            ArgumentCaptor<AgreementCancelledEvent> captor =
                    ArgumentCaptor.forClass(AgreementCancelledEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AgreementCancelledEvent event = captor.getValue();
            assertThat(event.agreementId()).isEqualTo(AGREEMENT_ID);
            assertThat(event.pitchId()).isEqualTo(1L);
            assertThat(event.hostId()).isEqualTo(HOST_ID);
            assertThat(event.creatorId()).isEqualTo(CREATOR_ID);
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("outsider cannot cancel — throws CollaborationAccessDeniedException")
        void outsider_cannot_cancel() {
            CollaborationAgreement agreement = activeAgreement();
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

            assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, 999L))
                    .isInstanceOf(CollaborationAccessDeniedException.class);

            verify(agreementRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // ── State guard ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("state guard")
    class StateGuard {

        @Test
        @DisplayName("cannot cancel a COMPLETED agreement — throws IllegalStateException")
        void throws_when_completed() {
            CollaborationAgreement agreement = completedAgreement();
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

            assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, HOST_ID))
                    .isInstanceOf(IllegalStateException.class);

            verify(agreementRepository, never()).save(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws AgreementNotFoundException when agreement not found")
    void throws_when_not_found() {
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, HOST_ID))
                .isInstanceOf(AgreementNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CollaborationAgreement activeAgreement() {
        return CollaborationAgreement.builder()
                .id(AGREEMENT_ID)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(AgreementStatus.ACTIVE)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(List.of())
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }

    private CollaborationAgreement completedAgreement() {
        return CollaborationAgreement.builder()
                .id(AGREEMENT_ID)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(AgreementStatus.COMPLETED)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(List.of())
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }
}
