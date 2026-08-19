package com.affiliate.rentals.gydi.collaborations.application.usecase;

import com.affiliate.rentals.gydi.collaborations.application.port.out.AgreementRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.port.out.DeliveryAssetRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.application.usecase.command.UploadDeliveryAssetCommand;
import com.affiliate.rentals.gydi.collaborations.domain.event.DeliverySubmittedEvent;
import com.affiliate.rentals.gydi.collaborations.domain.exception.AgreementNotFoundException;
import com.affiliate.rentals.gydi.collaborations.domain.exception.CollaborationAccessDeniedException;
import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.DeliveryAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case to register a pre-uploaded delivery asset for an agreement deliverable.
 *
 * <p>The file has already been uploaded to storage before calling this use case.
 * This use case records the asset in the database and publishes {@link DeliverySubmittedEvent}.
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadDeliveryAssetUseCase {

    private final AgreementRepositoryPort agreementRepository;
    private final DeliveryAssetRepositoryPort deliveryAssetRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DeliveryAsset execute(UploadDeliveryAssetCommand command) {
        CollaborationAgreement agreement = agreementRepository.findById(command.agreementId())
                .orElseThrow(() -> AgreementNotFoundException.withId(command.agreementId()));

        if (!agreement.creatorId().equals(command.requestingUserId())) {
            throw new CollaborationAccessDeniedException(
                    "Only the creator may upload delivery assets for agreement " + command.agreementId());
        }

        findDeliverable(agreement, command.deliverableId()); // validate deliverable belongs to agreement

        DeliveryAsset asset = DeliveryAsset.create(
                command.agreementId(),
                command.deliverableId(),
                command.fileUrl(),
                command.fileType(),
                command.fileSizeBytes()
        );

        DeliveryAsset saved = deliveryAssetRepository.save(asset);

        eventPublisher.publishEvent(new DeliverySubmittedEvent(
                agreement.id(),
                command.deliverableId(),
                saved.id(),
                agreement.hostId(),
                agreement.creatorId()
        ));

        log.info("Delivery asset uploaded: assetId={}, agreementId={}, deliverableId={}",
                saved.id(), command.agreementId(), command.deliverableId());
        return saved;
    }

    private AgreementDeliverable findDeliverable(CollaborationAgreement agreement, Long deliverableId) {
        return agreement.deliverables().stream()
                .filter(d -> deliverableId.equals(d.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deliverable " + deliverableId + " not found in agreement " + agreement.id()));
    }
}
