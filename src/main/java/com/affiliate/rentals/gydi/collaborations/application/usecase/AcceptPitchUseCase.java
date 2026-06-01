package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.AcceptPitchCommand;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.AcceptPitchResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchAcceptedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.PitchNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to accept a pitch (or the current counter-offer state).
 *
 * <p>Verifies the requesting user is the correct next responder,
 * triggers agreement generation, and publishes {@link PitchAcceptedEvent}.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptPitchUseCase {

    private final PitchRepositoryPort pitchRepository;
    private final GenerateAgreementUseCase generateAgreement;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AcceptPitchResult execute(AcceptPitchCommand command) {
        CreatorPitch pitch = pitchRepository.findById(command.pitchId())
                .orElseThrow(() -> PitchNotFoundException.withId(command.pitchId()));

        verifyResponder(pitch, command.requestingUserId());

        pitch.accept();
        CreatorPitch saved = pitchRepository.save(pitch);

        CollaborationAgreement agreement = generateAgreement.execute(saved.id());

        eventPublisher.publishEvent(
                new PitchAcceptedEvent(saved.id(), agreement.id(), saved.hostId(), saved.creatorId()));

        log.info("Pitch accepted: pitchId={}, agreementId={}", saved.id(), agreement.id());
        return new AcceptPitchResult(saved.id(), saved.status(), agreement.id());
    }

    /**
     * The requesting user must be either the host or the creator AND it must be their turn to respond.
     * The next responder is determined by {@link CreatorPitch#nextResponder()}.
     */
    private void verifyResponder(CreatorPitch pitch, Long requestingUserId) {
        boolean isHost = pitch.hostId().equals(requestingUserId);
        boolean isCreator = pitch.creatorId().equals(requestingUserId);

        if (!isHost && !isCreator) {
            throw new CollaborationAccessDeniedException(
                    "User " + requestingUserId + " is not a party to pitch " + pitch.id());
        }

        OfferedBy nextResponder = pitch.nextResponder();
        boolean isHostTurn = nextResponder == OfferedBy.HOST && isHost;
        boolean isCreatorTurn = nextResponder == OfferedBy.CREATOR && isCreator;

        if (!isHostTurn && !isCreatorTurn) {
            throw new CollaborationAccessDeniedException(
                    "It is not your turn to respond. Next responder: " + nextResponder);
        }
    }
}
