package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CreatePitchCommand;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CreatorNotVerifiedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.DuplicatePitchException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.MonthlyPitchLimitException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePitchUseCase")
class CreatePitchUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private PropertyRepositoryPort propertyRepository;
    @Mock private PitchRepositoryPort pitchRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreatePitchUseCase useCase;

    private static final Long CREATOR_ID = 20L;
    private static final Long HOST_ID    = 10L;
    private static final Long PROPERTY_ID = 1L;

    private CreatePitchCommand validCommand;

    @BeforeEach
    void setUp() {
        useCase = new CreatePitchUseCase(userRepository, propertyRepository, pitchRepository, eventPublisher);
        validCommand = new CreatePitchCommand(
                PROPERTY_ID, CREATOR_ID,
                "Amazing lifestyle content for your property",
                "https://portfolio.example.com",
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(37),
                List.of(),
                PitchCompensation.freeStay(7)
        );
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("saves pitch and publishes PitchReceivedEvent when all guards pass")
        void saves_and_publishes_event() {
            User verifiedCreator = mockVerifiedCreator();
            Property openProperty = mockOpenProperty();
            CreatorPitch savedPitch = mockSavedPitch();

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(PropertyId.of(PROPERTY_ID))).thenReturn(Optional.of(openProperty));
            when(pitchRepository.existsActivePitch(CREATOR_ID, PROPERTY_ID)).thenReturn(false);
            when(pitchRepository.countByCreatorIdSince(eq(CREATOR_ID), any())).thenReturn(0);
            when(pitchRepository.save(any())).thenReturn(savedPitch);

            CreatorPitch result = useCase.execute(validCommand);

            assertThat(result).isEqualTo(savedPitch);
            verify(eventPublisher).publishEvent(any(PitchReceivedEvent.class));
        }

        @Test
        @DisplayName("published event carries correct pitchId and hostId")
        void event_carries_correct_ids() {
            User verifiedCreator = mockVerifiedCreator();
            Property openProperty = mockOpenProperty();
            CreatorPitch savedPitch = mockSavedPitch();

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(openProperty));
            when(pitchRepository.existsActivePitch(any(), any())).thenReturn(false);
            when(pitchRepository.countByCreatorIdSince(any(), any())).thenReturn(0);
            when(pitchRepository.save(any())).thenReturn(savedPitch);

            useCase.execute(validCommand);

            ArgumentCaptor<PitchReceivedEvent> captor = ArgumentCaptor.forClass(PitchReceivedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            PitchReceivedEvent event = captor.getValue();
            assertThat(event.pitchId()).isEqualTo(savedPitch.id());
            assertThat(event.hostId()).isEqualTo(HOST_ID);
            assertThat(event.creatorId()).isEqualTo(CREATOR_ID);
        }
    }

    // ── Guard: creator must be verified ───────────────────────────────────────

    @Nested
    @DisplayName("creator verification guard")
    class CreatorVerification {

        @Test
        @DisplayName("throws CreatorNotVerifiedException when creator is not verified")
        void throws_when_not_verified() {
            User unverified = mock(User.class);
            when(unverified.isVerifiedCreator()).thenReturn(false);
            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(unverified));

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(CreatorNotVerifiedException.class);

            verify(pitchRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when creator not found")
        void throws_when_creator_not_found() {
            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(CREATOR_ID.toString());
        }
    }

    // ── Guard: property must accept collaborations ─────────────────────────────

    @Nested
    @DisplayName("property collaboration guard")
    class PropertyCollaboration {

        @Test
        @DisplayName("throws when property does not accept collaborations")
        void throws_when_property_closed() {
            User verifiedCreator = mockVerifiedCreator();
            Property closedProperty = mock(Property.class);
            when(closedProperty.isAcceptCreatorCollaborations()).thenReturn(false);

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(closedProperty));

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not accept collaborations");

            verify(pitchRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when property not found")
        void throws_when_property_not_found() {
            User verifiedCreator = mockVerifiedCreator();
            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(PROPERTY_ID.toString());
        }
    }

    // ── Guard: no duplicate active pitch ──────────────────────────────────────

    @Nested
    @DisplayName("duplicate pitch guard")
    class DuplicatePitch {

        @Test
        @DisplayName("throws DuplicatePitchException when active pitch exists")
        void throws_when_duplicate() {
            User verifiedCreator = mockVerifiedCreator();
            Property openProperty = mockOpenProperty();

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(openProperty));
            when(pitchRepository.existsActivePitch(CREATOR_ID, PROPERTY_ID)).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(DuplicatePitchException.class);

            verify(pitchRepository, never()).save(any());
        }
    }

    // ── Guard: monthly limit ───────────────────────────────────────────────────

    @Nested
    @DisplayName("monthly pitch limit guard")
    class MonthlyLimit {

        @Test
        @DisplayName("throws MonthlyPitchLimitException when creator reached 10 pitches this month")
        void throws_when_limit_reached() {
            User verifiedCreator = mockVerifiedCreator();
            Property openProperty = mockOpenProperty();

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(openProperty));
            when(pitchRepository.existsActivePitch(any(), any())).thenReturn(false);
            when(pitchRepository.countByCreatorIdSince(eq(CREATOR_ID), any())).thenReturn(10);

            assertThatThrownBy(() -> useCase.execute(validCommand))
                    .isInstanceOf(MonthlyPitchLimitException.class);

            verify(pitchRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows pitch when creator has 9 pitches this month")
        void allows_at_limit_minus_one() {
            User verifiedCreator = mockVerifiedCreator();
            Property openProperty = mockOpenProperty();
            CreatorPitch savedPitch = mockSavedPitch();

            when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(verifiedCreator));
            when(propertyRepository.findById(any())).thenReturn(Optional.of(openProperty));
            when(pitchRepository.existsActivePitch(any(), any())).thenReturn(false);
            when(pitchRepository.countByCreatorIdSince(eq(CREATOR_ID), any())).thenReturn(9);
            when(pitchRepository.save(any())).thenReturn(savedPitch);

            assertThatNoException().isThrownBy(() -> useCase.execute(validCommand));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User mockVerifiedCreator() {
        User u = mock(User.class);
        when(u.isVerifiedCreator()).thenReturn(true);
        return u;
    }

    private Property mockOpenProperty() {
        Property p = mock(Property.class);
        when(p.isAcceptCreatorCollaborations()).thenReturn(true);
        lenient().when(p.getHostId()).thenReturn(HOST_ID);
        return p;
    }

    private CreatorPitch mockSavedPitch() {
        CreatorPitch p = mock(CreatorPitch.class);
        lenient().when(p.id()).thenReturn(100L);
        lenient().when(p.propertyId()).thenReturn(PROPERTY_ID);
        lenient().when(p.hostId()).thenReturn(HOST_ID);
        lenient().when(p.creatorId()).thenReturn(CREATOR_ID);
        lenient().when(p.status()).thenReturn(PitchStatus.PENDING);
        return p;
    }
}
