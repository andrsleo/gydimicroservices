package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.collaborations.application.port.out.DeliveryAssetRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.model.DeliveryAsset;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.DeliveryAssetEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.DeliveryAssetJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeliveryAssetRepositoryAdapter implements DeliveryAssetRepositoryPort {

    private final DeliveryAssetJpaRepository jpaRepository;

    @Override
    public DeliveryAsset save(DeliveryAsset asset) {
        DeliveryAssetEntity entity = toEntity(asset);
        DeliveryAssetEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryAsset> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DeliveryAsset> findByDeliverableId(Long agreementDeliverableId) {
        return jpaRepository.findByAgreementDeliverableId(agreementDeliverableId).stream()
                .map(this::toDomain)
                .toList();
    }

    private DeliveryAsset toDomain(DeliveryAssetEntity e) {
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

    private DeliveryAssetEntity toEntity(DeliveryAsset domain) {
        DeliveryAssetEntity entity = new DeliveryAssetEntity();
        entity.setId(domain.id());
        entity.setAgreementId(domain.agreementId());
        entity.setAgreementDeliverableId(domain.agreementDeliverableId());
        entity.setFileUrl(domain.fileUrl());
        entity.setFileType(domain.fileType());
        entity.setFileSizeBytes(domain.fileSizeBytes());
        entity.setReviewStatus(domain.reviewStatus());
        entity.setUploadedAt(domain.uploadedAt());
        return entity;
    }
}
