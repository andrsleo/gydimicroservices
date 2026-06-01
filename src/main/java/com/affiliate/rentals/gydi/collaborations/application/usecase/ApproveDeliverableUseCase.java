package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.ApproveDeliverableResult;
import com.affiliate.rentals.gydi.collaborations.domain.event.DeliveryApprovedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case to approve a deliverable submission.
 *
 * <p>Only the host may approve deliverables. If all deliverables become approved,
 * the agreement transitions to COMPLETED.
 * Publishes {@link DeliveryApprovedEvent} on success.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveDeliverableUseCase {

    private final AgreementRepositoryPort agreementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ApproveDeliverableResult execute(Long agreementId, Long deliverableId, Long requestingUserId) {
        CollaborationAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> AgreementNotFoundException.withId(agreementId));

        if (!agreement.hostId().equals(requestingUserId)) {
            throw new CollaborationAccessDeniedException(
                    "Only the host may approve deliverables for agreement " + agreementId);
        }

        AgreementDeliverable deliverable = findDeliverable(agreement, deliverableId);
        deliverable.approve();

        boolean allApproved = agreement.deliverables().stream().allMatch(AgreementDeliverable::isApproved);
        if (allApproved) {
            agreement.complete();
        }

        CollaborationAgreement saved = agreementRepository.save(agreement);

        eventPublisher.publishEvent(new DeliveryApprovedEvent(
                agreement.id(),
                deliverableId,
                agreement.creatorId(),
                allApproved
        ));

        log.info("Deliverable approved: agreementId={}, deliverableId={}, allApproved={}",
                agreementId, deliverableId, allApproved);

        return new ApproveDeliverableResult(deliverableId, deliverable.status(), saved.status());
    }

    private AgreementDeliverable findDeliverable(CollaborationAgreement agreement, Long deliverableId) {
        return agreement.deliverables().stream()
                .filter(d -> deliverableId.equals(d.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deliverable " + deliverableId + " not found in agreement " + agreement.id()));
    }
}
