package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelPitchUseCase")
class CancelPitchUseCaseTest {

    @Mock private PitchRepositoryPort pitchRepository;

    private CancelPitchUseCase useCase;

    private static final Long PITCH_ID   = 1L;
    private static final Long HOST_ID    = 10L;
    private static final Long CREATOR_ID = 20L;

    @BeforeEach
    void setUp() {
        useCase = new CancelPitchUseCase(pitchRepository);
    }

    @Test
    @DisplayName("creator cancels PENDING pitch — saves cancelled pitch")
    void creator_cancels_pending() {
        CreatorPitch pitch = pendingPitch();
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));
        when(pitchRepository.save(any())).thenReturn(pitch);

        assertThatNoException().isThrownBy(() -> useCase.execute(PITCH_ID, CREATOR_ID));

        verify(pitchRepository).save(argThat(p -> p.status() == PitchStatus.CANCELLED));
    }

    @Test
    @DisplayName("host cannot cancel — only creator can")
    void host_cannot_cancel() {
        CreatorPitch pitch = pendingPitch();
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.of(pitch));

        assertThatThrownBy(() -> useCase.execute(PITCH_ID, HOST_ID))
                .isInstanceOf(CollaborationAccessDeniedException.class)
                .hasMessageContaining("Only the creator");

        verify(pitchRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws PitchNotFoundException when pitch not found")
    void throws_when_not_found() {
        when(pitchRepository.findById(PITCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(PITCH_ID, CREATOR_ID))
                .isInstanceOf(PitchNotFoundException.class);
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
}
