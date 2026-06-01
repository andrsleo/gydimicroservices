package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.ApproveDeliverableResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.DeliveryApprovedEvent;
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
@DisplayName("ApproveDeliverableUseCase")
class ApproveDeliverableUseCaseTest {

    @Mock private AgreementRepositoryPort agreementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ApproveDeliverableUseCase useCase;

    private static final Long AGREEMENT_ID   = 50L;
    private static final Long DELIVERABLE_ID = 101L;
    private static final Long HOST_ID        = 10L;
    private static final Long CREATOR_ID     = 20L;

    @BeforeEach
    void setUp() {
        useCase = new ApproveDeliverableUseCase(agreementRepository, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host approves single deliverable → APPROVED, event published, allApproved=false")
        void host_approves_one_of_two() {
            AgreementDeliverable d1 = submittedDeliverable(DELIVERABLE_ID);
            AgreementDeliverable d2 = submittedDeliverable(102L);
            CollaborationAgreement agreement = activeAgreement(List.of(d1, d2));

            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            ApproveDeliverableResult result = useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, HOST_ID);

            assertThat(result.deliverableId()).isEqualTo(DELIVERABLE_ID);
            assertThat(result.deliverableStatus()).isEqualTo("APPROVED");
            // agreement still ACTIVE — not all approved
            assertThat(result.agreementStatus()).isEqualTo(AgreementStatus.ACTIVE);

            ArgumentCaptor<DeliveryApprovedEvent> captor = ArgumentCaptor.forClass(DeliveryApprovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().allApproved()).isFalse();
        }

        @Test
        @DisplayName("approving last deliverable completes the agreement")
        void last_approval_completes_agreement() {
            AgreementDeliverable d1 = submittedDeliverable(DELIVERABLE_ID);
            // agreement must be in DELIVERED status before complete() can be called
            CollaborationAgreement agreement = deliveredAgreement(List.of(d1));

            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
            when(agreementRepository.save(any())).thenReturn(agreement);

            useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, HOST_ID);

            // agreement.complete() is called → status = COMPLETED
            assertThat(agreement.status()).isEqualTo(AgreementStatus.COMPLETED);

            ArgumentCaptor<DeliveryApprovedEvent> captor = ArgumentCaptor.forClass(DeliveryApprovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().allApproved()).isTrue();
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("creator cannot approve — only host can")
        void creator_cannot_approve() {
            AgreementDeliverable d1 = submittedDeliverable(DELIVERABLE_ID);
            CollaborationAgreement agreement = activeAgreement(List.of(d1));
            when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

            assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, CREATOR_ID))
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

        assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, DELIVERABLE_ID, HOST_ID))
                .isInstanceOf(AgreementNotFoundException.class);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when deliverable not in agreement")
    void throws_when_deliverable_not_found() {
        AgreementDeliverable d1 = submittedDeliverable(DELIVERABLE_ID);
        CollaborationAgreement agreement = activeAgreement(List.of(d1));
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() -> useCase.execute(AGREEMENT_ID, 999L, HOST_ID))
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

    private CollaborationAgreement deliveredAgreement(List<AgreementDeliverable> deliverables) {
        return CollaborationAgreement.builder()
                .id(AGREEMENT_ID)
                .pitchId(1L)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .status(AgreementStatus.DELIVERED)
                .checkInDate(LocalDate.now().plusDays(30))
                .checkOutDate(LocalDate.now().plusDays(37))
                .deliverables(deliverables)
                .contentRights(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .build();
    }
}
