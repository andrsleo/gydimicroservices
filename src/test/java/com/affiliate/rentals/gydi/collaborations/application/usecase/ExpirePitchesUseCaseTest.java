package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchExpiredEvent;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpirePitchesUseCase")
class ExpirePitchesUseCaseTest {

    @Mock private PitchRepositoryPort pitchRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ExpirePitchesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpirePitchesUseCase(pitchRepository, eventPublisher);
    }

    @Test
    @DisplayName("when no overdue pitches — does nothing")
    void no_overdue_pitches_nothing_happens() {
        when(pitchRepository.findExpired(any())).thenReturn(List.of());

        useCase.execute();

        verify(pitchRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("expires each overdue pitch and publishes PitchExpiredEvent per pitch")
    void expires_each_overdue_pitch() {
        CreatorPitch pitch1 = pendingPitchWithId(1L, 10L, 20L);
        CreatorPitch pitch2 = pendingPitchWithId(2L, 11L, 21L);

        when(pitchRepository.findExpired(any())).thenReturn(List.of(pitch1, pitch2));
        when(pitchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute();

        verify(pitchRepository, times(2)).save(any());

        ArgumentCaptor<PitchExpiredEvent> captor = ArgumentCaptor.forClass(PitchExpiredEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<PitchExpiredEvent> events = captor.getAllValues();
        assertThat(events).extracting(PitchExpiredEvent::pitchId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("pitch status is EXPIRED after expiration run")
    void pitch_is_expired_after_run() {
        CreatorPitch pitch = pendingPitchWithId(1L, 10L, 20L);
        when(pitchRepository.findExpired(any())).thenReturn(List.of(pitch));
        when(pitchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute();

        ArgumentCaptor<CreatorPitch> saved = ArgumentCaptor.forClass(CreatorPitch.class);
        verify(pitchRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(PitchStatus.EXPIRED);
    }

    private CreatorPitch pendingPitchWithId(Long pitchId, Long hostId, Long creatorId) {
        return CreatorPitch.builder()
                .id(pitchId)
                .propertyId(1L)
                .hostId(hostId)
                .creatorId(creatorId)
                .introduction("intro")
                .preferredCheckIn(LocalDate.now().plusDays(30))
                .preferredCheckOut(LocalDate.now().plusDays(37))
                .status(PitchStatus.PENDING)
                .counterOfferRounds(0)
                .deliverables(List.of())
                .compensation(PitchCompensation.freeStay(7))
                .counterOffers(List.of())
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();
    }
}
