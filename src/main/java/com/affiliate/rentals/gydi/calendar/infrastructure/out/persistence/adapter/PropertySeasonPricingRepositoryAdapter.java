package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.calendar.domain.model.PropertySeasonPricing;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.out.PropertySeasonPricingRepositoryPort;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.PropertySeasonPricingJpaEntity;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository.PropertySeasonPricingJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PropertySeasonPricingRepositoryAdapter implements PropertySeasonPricingRepositoryPort {

    private final PropertySeasonPricingJpaRepository jpaRepository;

    public PropertySeasonPricingRepositoryAdapter(PropertySeasonPricingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<PropertySeasonPricing> findByPropertyId(Long propertyId) {
        return jpaRepository.findByPropertyId(propertyId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<SeasonType, BigDecimal> findMultipliersByPropertyId(Long propertyId) {
        return jpaRepository.findByPropertyId(propertyId)
                .stream()
                .collect(Collectors.toMap(
                        PropertySeasonPricingJpaEntity::getSeasonType,
                        PropertySeasonPricingJpaEntity::getPriceMultiplier
                ));
    }

    @Override
    public PropertySeasonPricing save(PropertySeasonPricing pricing) {
        PropertySeasonPricingJpaEntity entity = toEntity(pricing);
        PropertySeasonPricingJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void saveAll(List<PropertySeasonPricing> pricings) {
        List<PropertySeasonPricingJpaEntity> entities = pricings.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public void deleteByPropertyId(Long propertyId) {
        jpaRepository.deleteByPropertyId(propertyId);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private PropertySeasonPricingJpaEntity toEntity(PropertySeasonPricing domain) {
        return PropertySeasonPricingJpaEntity.builder()
                .id(domain.getId())
                .propertyId(domain.getPropertyId())
                .seasonType(domain.getSeasonType())
                .priceMultiplier(domain.getPriceMultiplier())
                .createdAt(domain.getCreatedAt().toOffsetDateTime())
                .updatedAt(domain.getUpdatedAt().toOffsetDateTime())
                .build();
    }

    private PropertySeasonPricing toDomain(PropertySeasonPricingJpaEntity entity) {
        return PropertySeasonPricing.reconstruct(
                entity.getId(),
                entity.getPropertyId(),
                SeasonType.valueOf(entity.getSeasonType().name()),
                entity.getPriceMultiplier(),
                entity.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC)
        );
    }
}
