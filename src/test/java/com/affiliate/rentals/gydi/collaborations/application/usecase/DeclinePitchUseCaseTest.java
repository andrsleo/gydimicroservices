package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.DeclinePitchCommand;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.DeclinePitchResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchDeclinedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
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
@DisplayName("DeclinePitchUseCase")
class DeclinePitchUseCaseTest {

    @Mock private PitchRepositoryPort pitchRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DeclinePitchUseCase useCase;

    private static final Long PITCH_ID   = 1L;
    private static final Long HOST_ID    = 10L;
    private static final Long CREATOR_ID = 20L;

    @BeforeEach
    void setUp() {
        useCase = new DeclinePitchUseCase(pitchRepository, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host declines PENDING pitch → DECLINED, event published")
        void host_declines_with_reason() {
            CreatorPitch pitch = pendingPitch();
            CreatorPitch saved = savedDeclinedPitch("Not the right fit");

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(pitchRepository.save(any())).thenReturn(saved);

            DeclinePitchResult result = useCase.execute(
                    new DeclinePitchCommand(PITCH_ID, HOST_ID, "Not the right fit"));

            assertThat(result.pitchId()).isEqualTo(PITCH_ID);
            assertThat(result.status()).isEqualTo(PitchStatus.DECLINED);

            ArgumentCaptor<PitchDeclinedEvent> captor = ArgumentCaptor.forClass(PitchDeclinedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().reason()).isEqualTo("Not the right fit");
        }

        @Test
        @DisplayName("null reason is accepted (reason is optional)")
        void null_reason_accepted() {
            CreatorPitch pitch = pendingPitch();
            CreatorPitch saved = savedDeclinedPitch(null);

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(pitchRepository.save(any())).thenReturn(saved);

            assertThatNoException().isThrownBy(() ->
                    useCase.execute(new DeclinePitchCommand(PITCH_ID, HOST_ID, null)));
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("throws CollaborationAccessDeniedException for non-party user")
        void throws_for_outsider() {
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            assertThatThrownBy(() -> useCase.execute(new DeclinePitchCommand(PITCH_ID, 999L, null)))
                    .isInstanceOf(CollaborationAccessDeniedException.class);

            verify(pitchRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when creator tries to decline on PENDING (host's turn)")
        void throws_when_wrong_turn() {
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            assertThatThrownBy(() -> useCase.execute(new DeclinePitchCommand(PITCH_ID, CREATOR_ID, null)))
                    .isInstanceOf(CollaborationAccessDeniedException.class)
                    .hasMessageContaining("not your turn");

            verify(pitchRepository, never()).save(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws PitchNotFoundException when pitch does not exist")
    void throws_when_not_found() {
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new DeclinePitchCommand(PITCH_ID, HOST_ID, null)))
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

    private CreatorPitch savedDeclinedPitch(String reason) {
        CreatorPitch p = mock(CreatorPitch.class);
        lenient().when(p.id()).thenReturn(PITCH_ID);
        lenient().when(p.hostId()).thenReturn(HOST_ID);
        lenient().when(p.creatorId()).thenReturn(CREATOR_ID);
        lenient().when(p.status()).thenReturn(PitchStatus.DECLINED);
        lenient().when(p.declinedReason()).thenReturn(reason);
        return p;
    }
}
