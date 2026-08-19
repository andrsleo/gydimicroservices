package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.CreatePitchCommand;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchReceivedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CreatorNotVerifiedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.DuplicatePitchException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.MonthlyPitchLimitException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Use case to submit a new creator pitch for a property.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Creator must be verified ({@link CreatorNotVerifiedException})</li>
 *   <li>Property must accept creator collaborations (422)</li>
 *   <li>No duplicate active pitch for the same creator + property ({@link DuplicatePitchException})</li>
 *   <li>Monthly limit of 10 pitches per creator ({@link MonthlyPitchLimitException})</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePitchUseCase {

    private static final int MONTHLY_PITCH_LIMIT = 10;

    private final UserRepositoryPort userRepository;
    private final PropertyRepositoryPort propertyRepository;
    private final PitchRepositoryPort pitchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreatorPitch execute(CreatePitchCommand command) {
        User creator = userRepository.findById(command.creatorId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.creatorId()));

        if (!creator.isVerifiedCreator()) {
            throw new CreatorNotVerifiedException();
        }

        Property property = propertyRepository.findById(PropertyId.of(command.propertyId()))
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + command.propertyId()));

        if (!property.isAcceptCreatorCollaborations()) {
            throw new IllegalStateException("Property does not accept collaborations");
        }

        if (pitchRepository.existsActivePitch(command.creatorId(), command.propertyId())) {
            throw new DuplicatePitchException(command.creatorId(), command.propertyId());
        }

        OffsetDateTime firstDayOfCurrentMonth = OffsetDateTime.now(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        int monthlyCount = pitchRepository.countByCreatorIdSince(command.creatorId(), firstDayOfCurrentMonth);
        if (monthlyCount >= MONTHLY_PITCH_LIMIT) {
            throw new MonthlyPitchLimitException();
        }

        CreatorPitch pitch = CreatorPitch.create(
                command.propertyId(),
                property.getHostId(),
                command.creatorId(),
                command.introduction(),
                command.portfolioUrl(),
                command.preferredCheckIn(),
                command.preferredCheckOut(),
                command.deliverables(),
                command.compensation()
        );

        CreatorPitch saved = pitchRepository.save(pitch);
        eventPublisher.publishEvent(
                new PitchReceivedEvent(saved.id(), saved.propertyId(), saved.hostId(), saved.creatorId()));

        log.info("Pitch submitted: pitchId={}, creatorId={}, propertyId={}",
                saved.id(), saved.creatorId(), saved.propertyId());
        return saved;
    }
}
