package com.affiliate.rentals.gydi.referrals.infrastructure.out.persistence;

import com.affiliate.rentals.gydi.referrals.domain.model.AttributionType;
import com.affiliate.rentals.gydi.referrals.domain.model.ContentAttribution;
import com.affiliate.rentals.gydi.referrals.domain.port.ContentAttributionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContentAttributionRepositoryAdapter implements ContentAttributionRepositoryPort {

    private final ContentAttributionJpaRepository jpaRepository;

    @Override
    public ContentAttribution save(ContentAttribution attribution) {
        ContentAttributionJpaEntity entity = toEntity(attribution);
        ContentAttributionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ContentAttribution> findByCreatorId(Long creatorId) {
        return jpaRepository.findByCreatorId(creatorId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ContentAttribution> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId)
                .map(this::toDomain);
    }

    @Override
    public List<ContentAttribution> findByContentPostId(Long contentPostId) {
        return jpaRepository.findByContentPostId(contentPostId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countBookingsByContentPostId(Long contentPostId) {
        return jpaRepository.countBookingsByContentPostId(contentPostId);
    }

    @Override
    public List<Long> findTopCreatorIdsByPropertyId(Long propertyId) {
        return jpaRepository.findTopCreatorIdsByPropertyId(propertyId);
    }

    @Override
    public int countTotalBookingsByPropertyId(Long propertyId) {
        return jpaRepository.countTotalBookingsByPropertyId(propertyId);
    }

    private ContentAttributionJpaEntity toEntity(ContentAttribution domain) {
        ContentAttributionJpaEntity entity = new ContentAttributionJpaEntity();
        entity.setId(domain.getId());
        entity.setBookingId(domain.getBookingId());
        entity.setContentPostId(domain.getContentPostId());
        entity.setCreatorId(domain.getCreatorId());
        entity.setReferralLinkId(domain.getReferralLinkId());
        entity.setAttributionType(domain.getAttributionType().name());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    private ContentAttribution toDomain(ContentAttributionJpaEntity entity) {
        return ContentAttribution.reconstitute(
                entity.getId(),
                entity.getBookingId(),
                entity.getContentPostId(),
                entity.getCreatorId(),
                entity.getReferralLinkId(),
                AttributionType.valueOf(entity.getAttributionType()),
                entity.getCreatedAt()
        );
    }
}
