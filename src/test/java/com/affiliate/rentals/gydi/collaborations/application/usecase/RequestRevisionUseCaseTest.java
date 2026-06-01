package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.RequestRevisionResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.RevisionRequestedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;
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
@DisplayName("RequestRevisionUseCase")
class RequestRevisionUseCaseTest {

    @Mock private AgreementRepositoryPort agreementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RequestRevisionUseCase useCase;

    private static final Long AGREEMENT_ID   = 50L;
    private static final Long DELIVERABLE_ID = 101L;
    private static final Long HOST_ID        = 10L;
    private static final Long CREATOR_ID     = 20L;

    @BeforeEach
    void setUp() {
        useCase = new RequestRevisionUseCase(agreementRepository, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host requests revision → deliverable REVISION_REQUESTED, event published")
        void host_requests_revision() {
            AgreementDeliverable deliverable = submittedDeliverable(DELIVERABLE_ID);
            CollaborationAgreement agreement = activeAgreement(List.of(deliverable));

            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            RequestRevisionResult result = useCase.execute(
                    AGREEMENT_ID, DELIVERABLE_ID, HOST_ID, "Needs better lighting");

            assertThat(result.deliverableId()).isEqualTo(DELIVERABLE_ID);
            assertThat(result.deliverableStatus()).isEqualTo("REVISION_REQUESTED");
            assertThat(result.feedback()).isEqualTo("Needs better lighting");

            verify(agreementRepository).save(agreement);
        }

        @Test
        @DisplayName("published event carries correct ids and feedback")
        void event_carries_correct_data() {
            AgreementDeliverable deliverable = submittedDeliverable(DELIVERABLE_ID);
            CollaborationAgreement agreement = activeAgreement(List.of(deliverable));

            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, HOST_ID, "More close-ups please");

            ArgumentCaptor<RevisionRequestedEvent> captor =
                    ArgumentCaptor.forClass(RevisionRequestedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            RevisionRequestedEvent event = captor.getValue();
            assertThat(event.agreementId()).isEqualTo(AGREEMENT_ID);
            assertThat(event.deliverableId()).isEqualTo(DELIVERABLE_ID);
            assertThat(event.creatorId()).isEqualTo(CREATOR_ID);
            assertThat(event.feedback()).isEqualTo("More close-ups please");
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("creator cannot request revision — only host can")
        void creator_cannot_request_revision() {
            AgreementDeliverable deliverable = submittedDeliverable(DELIVERABLE_ID);
            CollaborationAgreement agreement = activeAgreement(List.of(deliverable));
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

            assertThatThrownBy(() ->
                    useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, CREATOR_ID, "Feedback"))
                    .isInstanceOf(CollaborationAccessDeniedException.class)
                    .hasMessageContaining("Only the host");

            verify(agreementRepository, never()).save(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws AgreementNotFoundException when agreement not found")
    void throws_when_agreement_not_found() {
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, HOST_ID, "Feedback"))
                .isInstanceOf(AgreementNotFoundException.class);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when deliverable not in agreement")
    void throws_when_deliverable_not_found() {
        AgreementDeliverable deliverable = submittedDeliverable(DELIVERABLE_ID);
        CollaborationAgreement agreement = activeAgreement(List.of(deliverable));
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() ->
                useCase.execute(AGREEMENT_ID, 999L, HOST_ID, "Feedback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AgreementDeliverable submittedDeliverable(Long id) {
        return AgreementDeliverable.reconstitute(id, AGREEMENT_ID, DeliverableType.REEL,
                1, "SUBMITTED", null, List.of());
    }

    private CollaborationAgreement activeAgreement(List<AgreementDeliverable> deliverables) {
        return CollaborationAgreement.builder()
                .id(AGREEMENT_ID)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(AgreementStatus.ACTIVE)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(deliverables)
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }
}
