package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.event.PitchExpiredEvent;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Use case to expire pitches that have passed their 7-day response deadline.
 *
 * <p>Intended to be called by a scheduled task. Loads all pitches whose
 * {@code expiresAt} has passed, marks each as EXPIRED, saves, and publishes
 * {@link PitchExpiredEvent} for each.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirePitchesUseCase {

    private final PitchRepositoryPort pitchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute() {
        List<CreatorPitch> expired = pitchRepository.findExpired(OffsetDateTime.now());

        for (CreatorPitch pitch : expired) {
            pitch.expire();
            pitchRepository.save(pitch);
            eventPublisher.publishEvent(
                    new PitchExpiredEvent(pitch.id(), pitch.hostId(), pitch.creatorId()));
            log.info("Pitch expired: pitchId={}, creatorId={}, hostId={}",
                    pitch.id(), pitch.creatorId(), pitch.hostId());
        }

        log.info("Expiration run completed: {} pitches expired", expired.size());
    }
}
