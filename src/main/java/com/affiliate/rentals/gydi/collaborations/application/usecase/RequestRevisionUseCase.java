package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.RequestRevisionResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.RevisionRequestedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to request a revision on a submitted deliverable.
 *
 * <p>Only the host may request revisions. The deliverable transitions to REVISION_REQUESTED.
 * Publishes {@link RevisionRequestedEvent} on success.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestRevisionUseCase {

    private final AgreementRepositoryPort agreementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RequestRevisionResult execute(Long agreementId, Long deliverableId,
                                         Long requestingUserId, String feedback) {
        CollaborationAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> AgreementNotFoundException.withId(agreementId));

        if (!agreement.hostId().equals(requestingUserId)) {
            throw new CollaborationAccessDeniedException(
                    "Only the host may request revisions for agreement " + agreementId);
        }

        AgreementDeliverable deliverable = findDeliverable(agreement, deliverableId);
        deliverable.requestRevision(feedback);

        agreementRepository.save(agreement);

        eventPublisher.publishEvent(new RevisionRequestedEvent(
                agreement.id(),
                deliverableId,
                agreement.creatorId(),
                feedback
        ));

        log.info("Revision requested: agreementId={}, deliverableId={}", agreementId, deliverableId);

        return new RequestRevisionResult(deliverableId, deliverable.status(), feedback);
    }

    private AgreementDeliverable findDeliverable(CollaborationAgreement agreement, Long deliverableId) {
        return agreement.deliverables().stream()
                .filter(d -> deliverableId.equals(d.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deliverable " + deliverableId + " not found in agreement " + agreement.id()));
    }
}
