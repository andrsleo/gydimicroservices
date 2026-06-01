package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.CounterOfferRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CounterOfferResult;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CreateCounterOfferCommand;
import com.affiliate.rentals.gydi.collaborations.domain.event.CounterOfferReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CounterOffer;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType;
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
@DisplayName("CreateCounterOfferUseCase")
class CreateCounterOfferUseCaseTest {

    @Mock private PitchRepositoryPort pitchRepository;
    @Mock private CounterOfferRepositoryPort counterOfferRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateCounterOfferUseCase useCase;

    private static final Long PITCH_ID    = 1L;
    private static final Long HOST_ID     = 10L;
    private static final Long CREATOR_ID  = 20L;

    @BeforeEach
    void setUp() {
        useCase = new CreateCounterOfferUseCase(pitchRepository, counterOfferRepository, eventPublisher);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("host sends round-1 counter-offer → saved, event published with recipientId=creatorId")
        void host_sends_round1_counter_offer() {
            CreatorPitch pitch = pendingPitch();
            CounterOffer savedOffer = savedCounterOffer(200L, 1);

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(counterOfferRepository.save(any())).thenReturn(savedOffer);
            when(pitchRepository.save(any())).thenReturn(pitch);

            CreateCounterOfferCommand command = hostCommand(OfferedBy.HOST);
            CounterOfferResult result = useCase.execute(command);

            assertThat(result.pitchId()).isEqualTo(PITCH_ID);
            assertThat(result.counterOfferId()).isEqualTo(200L);
            assertThat(result.roundNumber()).isEqualTo(1);

            ArgumentCaptor<CounterOfferReceivedEvent> captor =
                    ArgumentCaptor.forClass(CounterOfferReceivedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().recipientId()).isEqualTo(CREATOR_ID);
        }

        @Test
        @DisplayName("creator sends round-2 counter-offer → recipientId is hostId")
        void creator_sends_round2_event_recipient_is_host() {
            // pitch is COUNTERED (host sent round 1) → creator's turn
            CreatorPitch pitch = counteredPitch(OfferedBy.HOST);
            CounterOffer savedOffer = savedCounterOffer(201L, 2);

            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
            when(counterOfferRepository.save(any())).thenReturn(savedOffer);
            when(pitchRepository.save(any())).thenReturn(pitch);

            CreateCounterOfferCommand command = creatorCommand(OfferedBy.CREATOR);
            useCase.execute(command);

            ArgumentCaptor<CounterOfferReceivedEvent> captor =
                    ArgumentCaptor.forClass(CounterOfferReceivedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().recipientId()).isEqualTo(HOST_ID);
        }
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("throws CollaborationAccessDeniedException for outsider")
        void throws_for_outsider() {
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            CreateCounterOfferCommand command = new CreateCounterOfferCommand(
                    PITCH_ID, 999L, OfferedBy.HOST, "offer", CompensationType.FREE_STAY,
                    7, null, null, null, List.of(), List.of());

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(CollaborationAccessDeniedException.class);

            verify(counterOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when it is not host's turn (creator already countered)")
        void throws_when_wrong_turn() {
            // pitch is COUNTERED with last offer by CREATOR → HOST's turn now
            CreatorPitch pitch = counteredPitch(OfferedBy.CREATOR);
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            // Host tries to claim CREATOR role — mismatch caught
            CreateCounterOfferCommand command = creatorCommand(OfferedBy.CREATOR);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(CollaborationAccessDeniedException.class);
        }

        @Test
        @DisplayName("throws when claimedBy role doesn't match actual user role")
        void throws_when_claimed_role_mismatch() {
            CreatorPitch pitch = pendingPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

            // CREATOR tries to claim HOST role
            CreateCounterOfferCommand command = new CreateCounterOfferCommand(
                    PITCH_ID, CREATOR_ID, OfferedBy.HOST, "offer", CompensationType.FREE_STAY,
                    7, null, null, null, List.of(), List.of());

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(CollaborationAccessDeniedException.class);
        }
    }

    // ── State guard ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("state guard")
    class StateGuard {

        @Test
        @DisplayName("throws IllegalStateException when pitch is CANCELLED (no counter-offer allowed)")
        void throws_when_pitch_cancelled() {
            CreatorPitch cancelled = cancelledPitch();
            when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(cancelled));

            CreateCounterOfferCommand command = hostCommand(OfferedBy.HOST);

            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(IllegalStateException.class);

            verify(counterOfferRepository, never()).save(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws PitchNotFoundException when pitch does not exist")
    void throws_when_not_found() {
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(hostCommand(OfferedBy.HOST)))
                .isInstanceOf(PitchNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateCounterOfferCommand hostCommand(OfferedBy offeredBy) {
        return new CreateCounterOfferCommand(
                PITCH_ID, HOST_ID, offeredBy, "Host counter-offer", CompensationType.FREE_STAY,
                5, null, null, null, List.of(), List.of());
    }

    private CreateCounterOfferCommand creatorCommand(OfferedBy offeredBy) {
        return new CreateCounterOfferCommand(
                PITCH_ID, CREATOR_ID, offeredBy, "Creator counter-offer", CompensationType.FREE_STAY,
                7, null, null, null, List.of(), List.of());
    }

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

    private CreatorPitch counteredPitch(OfferedBy lastOfferedBy) {
        CounterOffer existingOffer = CounterOffer.builder()
                .id(100L)
                .pitchId(PITCH_ID)
                .roundNumber(1)
                .offeredBy(lastOfferedBy)
                .compensationType(CompensationType.FREE_STAY)
                .nights(5)
                .build();

        return CreatorPitch.builder()
                .id(PITCH_ID)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .introduction("intro")
                .preferredCheckIn(LocalDate.now().plusDays(30))
                .preferredCheckOut(LocalDate.now().plusDays(37))
                .status(PitchStatus.COUNTERED)
                .counterOfferRounds(1)
                .deliverables(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .counterOffers(List.of(existingOffer))
                .build();
    }

    private CreatorPitch cancelledPitch() {
        return CreatorPitch.builder()
                .id(PITCH_ID)
                .propertyId(1L)
                .hostId(HOST_ID)
                .creatorId(CREATOR_ID)
                .introduction("intro")
                .preferredCheckIn(LocalDate.now().plusDays(30))
                .preferredCheckOut(LocalDate.now().plusDays(37))
                .status(PitchStatus.CANCELLED)
                .counterOfferRounds(0)
                .deliverables(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .counterOffers(List.of())
                .build();
    }

    private CounterOffer savedCounterOffer(Long id, int round) {
        return CounterOffer.builder()
                .id(id)
                .pitchId(PITCH_ID)
                .roundNumber(round)
                .offeredBy(OfferedBy.HOST)
                .compensationType(CompensationType.FREE_STAY)
                .nights(5)
                .build();
    }
}
