package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.collaborations.domain.model.AgreementDeliverable;
import com.affiliate.rentals.gydi.collaborations.domain.model.CollaborationAgreement;
import com.affiliate.rentals.gydi.collaborations.domain.model.ContentRight;
import com.affiliate.rentals.gydi.collaborations.domain.model.DeliveryAsset;
import com.affiliate.rentals.gydi.collaborations.domain.model.PitchCompensation;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.AgreementStatus;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.ContentRightType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.DeliverableType;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.OfferedBy;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.AgreementDeliverableEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.AgreementEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.ContentRightEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.DeliveryAssetEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchCompensationEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written mapper for CollaborationAgreement ↔ AgreementEntity conversion.
 *
 * <p>Note: The agreements table does not store compensation directly — it must be
 * loaded from the associated pitch's compensation record. Callers must supply
 * the compensation when converting to domain.
 */
public final class AgreementEntityMapper {

    private AgreementEntityMapper() {}

    // ── toDomain ─────────────────────────────────────────────────────────────

    /**
     * Converts AgreementEntity to domain model.
     * Compensation is sourced from the pitch; pass null if unavailable (will cause NPE at validation).
     */
    public static CollaborationAgreement toDomain(AgreementEntity entity,
                                                   PitchCompensationEntity compensationEntity) {
        PitchCompensation compensation = compensationEntity != null
                ? toCompensationDomain(compensationEntity)
                : null;

        List<AgreementDeliverable> deliverables = entity.getDeliverables() != null
                ? entity.getDeliverables().stream()
                        .map(AgreementEntityMapper::toDeliverableDomain)
                        .toList()
                : List.of();

        List<ContentRight> contentRights = entity.getContentRights() != null
                ? entity.getContentRights().stream()
                        .map(AgreementEntityMapper::toContentRightDomain)
                        .toList()
                : List.of();

        OfferedBy cancelledBy = entity.getCancelledBy() != null
                ? OfferedBy.valueOf(entity.getCancelledBy())
                : null;

        return CollaborationAgreement.builder()
                .id(entity.getId())
                .pitchId(entity.getPitchId())
                .propertyId(entity.getPropertyId())
                .hostId(entity.getHostId())
                .creatorId(entity.getCreatorId())
                .status(AgreementStatus.valueOf(entity.getStatus()))
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .deliveryDeadline(entity.getDeliveryDeadline())
                .postingDeadline(entity.getPostingDeadline())
                .cancellationPolicy(entity.getCancellationPolicy())
                .cancelledBy(cancelledBy)
                .cancelledAt(entity.getCancelledAt())
                .deliverables(deliverables)
                .contentRights(contentRights)
                .compensation(compensation)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static AgreementDeliverable toDeliverableDomain(AgreementDeliverableEntity e) {
        List<DeliveryAsset> assets = e.getAssets() != null
                ? e.getAssets().stream().map(AgreementEntityMapper::toDeliveryAssetDomain).toList()
                : List.of();

        return AgreementDeliverable.reconstitute(
                e.getId(),
                e.getAgreement().getId(),
                DeliverableType.valueOf(e.getDeliverableType()),
                e.getQuantity() != null ? e.getQuantity() : 0,
                e.getStatus(),
                e.getRevisionFeedback(),
                assets
        );
    }

    private static DeliveryAsset toDeliveryAssetDomain(DeliveryAssetEntity e) {
        return DeliveryAsset.reconstitute(
                e.getId(),
                e.getAgreementId(),
                e.getAgreementDeliverableId(),
                e.getFileUrl(),
                e.getFileType(),
                e.getFileSizeBytes() != null ? e.getFileSizeBytes() : 0L,
                e.getReviewStatus(),
                e.getUploadedAt()
        );
    }

    private static ContentRight toContentRightDomain(ContentRightEntity e) {
        ContentRightType type = ContentRightType.valueOf(e.getRightType().toUpperCase());
        return ContentRight.reconstitute(
                e.getId(),
                type,
                e.getDurationMonths() != null ? e.getDurationMonths().intValue() : null
        );
    }

    private static PitchCompensation toCompensationDomain(PitchCompensationEntity e) {
        com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType type =
                com.affiliate.rentals.gydi.collaborations.domain.model.enums.CompensationType
                        .valueOf(e.getCompensationType().toUpperCase());
        return PitchCompensation.reconstitute(
                type,
                e.getNights() != null ? e.getNights().intValue() : null,
                e.getAmount(),
                e.getCurrency(),
                e.getBookingCommissionPct(),
                e.getExperienceItems()
        );
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    public static AgreementEntity toEntity(CollaborationAgreement domain) {
        AgreementEntity entity = new AgreementEntity();
        entity.setId(domain.id());
        entity.setPitchId(domain.pitchId());
        entity.setPropertyId(domain.propertyId());
        entity.setHostId(domain.hostId());
        entity.setCreatorId(domain.creatorId());
        entity.setStatus(domain.status().name());
        entity.setCheckInDate(domain.checkInDate());
        entity.setCheckOutDate(domain.checkOutDate());
        entity.setDeliveryDeadline(domain.deliveryDeadline());
        entity.setPostingDeadline(domain.postingDeadline());
        entity.setCancellationPolicy(domain.cancellationPolicy());
        entity.setCancelledBy(domain.cancelledBy() != null ? domain.cancelledBy().name() : null);
        entity.setCancelledAt(domain.cancelledAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());

        // Deliverables
        List<AgreementDeliverableEntity> deliverableEntities = new ArrayList<>();
        for (AgreementDeliverable d : domain.deliverables()) {
            AgreementDeliverableEntity de = new AgreementDeliverableEntity();
            de.setId(d.id());
            de.setAgreement(entity);
            de.setDeliverableType(d.type().name());
            de.setQuantity((short) d.quantity());
            de.setStatus(d.status());
            de.setRevisionFeedback(d.revisionFeedback());
            // Assets are managed separately via DeliveryAssetRepositoryPort
            deliverableEntities.add(de);
        }
        entity.setDeliverables(deliverableEntities);

        // Content rights
        List<ContentRightEntity> rightEntities = new ArrayList<>();
        for (ContentRight r : domain.contentRights()) {
            ContentRightEntity re = new ContentRightEntity();
            re.setId(r.id());
            re.setAgreement(entity);
            re.setRightType(r.type().name().toLowerCase());
            re.setDurationMonths(r.durationMonths() != null ? r.durationMonths().shortValue() : null);
            rightEntities.add(re);
        }
        entity.setContentRights(rightEntities);

        return entity;
    }
}
