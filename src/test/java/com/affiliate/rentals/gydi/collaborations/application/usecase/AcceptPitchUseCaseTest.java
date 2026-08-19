package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.AcceptPitchCommand;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.AcceptPitchResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchAcceptedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
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
@DisplayName("AcceptPitchUseCase")
class AcceptPitchUseCaseTest {

    @Mock private PitchRepositoryPort pitchRepository;
    @Mock private GenerateAgreementUseCase generateAgreement;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AcceptPitchUseCase useCase;

    private static final Long PITCH_ID   = 1L;
    private static final Long HOST_ID    = 10L;
    private static final Long CREATOR_ID = 20L;
    private static final Long AGREEMENT_ID = 50L;

    @BeforeEach
    void setUp() {
        useCase = new AcceptPitchUseCase(pitchRepository, generateAgreement, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host accepts PENDING pitch → saves, generates agreement, publishes event")
        void host_accepts_pending_pitch() {
            CreatorPitch pitch = pendingPitch();
            CreatorPitch saved = savedPitch(PitchStatus.ACCEPTED);
            CollaborationAgreement agreement = mockAgreement();

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(pitchRepository.save(any())).thenReturn(saved);
            when(generateAgreement.execute(any())).thenReturn(agreement);

            AcceptPitchResult result = useCase.execute(new AcceptPitchCommand(PITCH_ID, HOST_ID));

            assertThat(result.pitchId()).isEqualTo(PITCH_ID);
            assertThat(result.agreementId()).isEqualTo(AGREEMENT_ID);
            verify(generateAgreement).execute(PITCH_ID);
            verify(eventPublisher).publishEvent(any(PitchAcceptedEvent.class));
        }

        @Test
        @DisplayName("published event carries pitchId, agreementId, hostId, creatorId")
        void event_carries_correct_ids() {
            CreatorPitch pitch = pendingPitch();
            CreatorPitch saved = savedPitch(PitchStatus.ACCEPTED);
            CollaborationAgreement agreement = mockAgreement();

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(pitchRepository.save(any())).thenReturn(saved);
            when(generateAgreement.execute(any())).thenReturn(agreement);

            useCase.execute(new AcceptPitchCommand(PITCH_ID, HOST_ID));

            ArgumentCaptor<PitchAcceptedEvent> captor = ArgumentCaptor.forClass(PitchAcceptedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            PitchAcceptedEvent event = captor.getValue();
            assertThat(event.pitchId()).isEqualTo(PITCH_ID);
            assertThat(event.agreementId()).isEqualTo(AGREEMENT_ID);
            assertThat(event.hostId()).isEqualTo(HOST_ID);
            assertThat(event.creatorId()).isEqualTo(CREATOR_ID);
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization guards")
    class Authorization {

        @Test
        @DisplayName("throws CollaborationAccessDeniedException when user is not a party")
        void throws_when_not_a_party() {
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            Long outsider = 999L;
            assertThatThrownBy(() -> useCase.execute(new AcceptPitchCommand(PITCH_ID, outsider)))
                    .isInstanceOf(CollaborationAccessDeniedException.class);

            verify(pitchRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when it is not the requesting user's turn (creator tries on PENDING)")
        void throws_when_wrong_turn() {
            // PENDING pitch → nextResponder() = HOST → creator cannot accept
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            assertThatThrownBy(() -> useCase.execute(new AcceptPitchCommand(PITCH_ID, CREATOR_ID)))
                    .isInstanceOf(CollaborationAccessDeniedException.class)
                    .hasMessageContaining("not your turn");

            verify(pitchRepository, never()).save(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws PitchNotFoundException when pitch does not exist")
    void throws_when_pitch_not_found() {
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new AcceptPitchCommand(PITCH_ID, HOST_ID)))
                .isInstanceOf(PitchNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatorPitch pendingPitch() {
        return CreatorPitch.builder()
                .id(PITCH_ID)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .introduction("intro")
                .preferredCheckIn(LocalDate.now().plusDays(30))
                .preferredCheckOut(LocalDate.now().plusDays(37))
                .status(PitchStatus.PENDING)
                .counterOfferRounds(0)
                .deliverables(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .counterOffers(List.of())
                .build();
    }

    private CreatorPitch savedPitch(PitchStatus status) {
        CreatorPitch p = mock(CreatorPitch.class);
        lenient().when(p.id()).thenReturn(PITCH_ID);
        lenient().when(p.hostId()).thenReturn(HOST_ID);
        lenient().when(p.creatorId()).thenReturn(CREATOR_ID);
        lenient().when(p.status()).thenReturn(status);
        return p;
    }

    private CollaborationAgreement mockAgreement() {
        CollaborationAgreement a = mock(CollaborationAgreement.class);
        lenient().when(a.id()).thenReturn(AGREEMENT_ID);
        lenient().when(a.status()).thenReturn(AgreementStatus.ACTIVE);
        return a;
    }
}
