package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.calendar.domain.model.SeasonDefinition;
import com.affiliate.rentals.gydi.calendar.domain.model.SeasonType;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.SeasonDefinitionJpaEntity;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository.SeasonDefinitionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SeasonDefinitionRepositoryAdapter implements SeasonDefinitionRepositoryPort {

    private final SeasonDefinitionJpaRepository jpaRepository;

    public SeasonDefinitionRepositoryAdapter(SeasonDefinitionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SeasonDefinition> findByCountry(String country) {
        return jpaRepository.findApplicableByCountry(country)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeasonDefinition> findByCountryAndDateRange(String country, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findApplicableByCountryAndDateRange(country, startDate, endDate)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeasonDefinition> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SeasonDefinition> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public SeasonDefinition save(SeasonDefinition season) {
        SeasonDefinitionJpaEntity entity = toEntity(season);
        SeasonDefinitionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private SeasonDefinition toDomain(SeasonDefinitionJpaEntity e) {
        return SeasonDefinition.reconstruct(
                e.getId(),
                e.getName(),
                SeasonType.valueOf(e.getSeasonType().name()),
                e.getScope() != null ? e.getScope() : "COUNTRY",
                e.getCountry(),
                e.getRegionCode(),
                e.getRegion(),
                e.getStartDate(),
                e.getEndDate(),
                e.getRecurrence(),
                e.isSystem(),
                e.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC),
                e.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC)
        );
    }

    private SeasonDefinitionJpaEntity toEntity(SeasonDefinition s) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return SeasonDefinitionJpaEntity.builder()
                .id(s.getId())
                .name(s.getName())
                .seasonType(s.getSeasonType())
                .scope(s.getScope())
                .country(s.getCountry())
                .regionCode(s.getRegionCode())
                .region(s.getRegion())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .recurrence(s.getRecurrence())
                .isSystem(s.isSystem())
                .createdAt(s.getCreatedAt() != null
                        ? s.getCreatedAt().toOffsetDateTime()
                        : now)
                .updatedAt(s.getUpdatedAt() != null
                        ? s.getUpdatedAt().toOffsetDateTime()
                        : now)
                .build();
    }
}
