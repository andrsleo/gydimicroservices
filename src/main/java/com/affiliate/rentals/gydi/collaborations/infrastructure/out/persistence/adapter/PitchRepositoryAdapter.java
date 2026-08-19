package com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.collaborations.application.port.out.PitchRepositoryPort;
import com.affiliate.rentals.gydi.collaborations.domain.model.CreatorPitch;
import com.affiliate.rentals.gydi.collaborations.domain.model.enums.PitchStatus;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.entity.PitchEntity;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.jpa.PitchJpaRepository;
import com.affiliate.rentals.gydi.collaborations.infrastructure.out.persistence.mapper.PitchEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PitchRepositoryAdapter implements PitchRepositoryPort {

    private final PitchJpaRepository jpaRepository;

    @Override
    public CreatorPitch save(CreatorPitch pitch) {
        PitchEntity entity = PitchEntityMapper.toEntity(pitch);
        PitchEntity saved = jpaRepository.save(entity);
        return PitchEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<CreatorPitch> findById(Long id) {
        return jpaRepository.findById(id).map(PitchEntityMapper::toDomain);
    }

    @Override
    public Page<CreatorPitch> findByCreatorId(Long creatorId, PitchStatus status, Pageable pageable) {
        if (status != null) {
            return jpaRepository.findByCreatorIdAndStatus(creatorId, status.name(), pageable)
                    .map(PitchEntityMapper::toDomain);
        }
        return jpaRepository.findByCreatorId(creatorId, pageable)
                .map(PitchEntityMapper::toDomain);
    }

    @Override
    public Page<CreatorPitch> findByHostId(Long hostId, PitchStatus status, Long propertyId, Pageable pageable) {
        if (propertyId != null) {
            return jpaRepository.findByHostIdAndPropertyIdAndStatus(
                            hostId, propertyId, status != null ? status.name() : null, pageable)
                    .map(PitchEntityMapper::toDomain);
        }
        if (status != null) {
            return jpaRepository.findByHostIdAndStatus(hostId, status.name(), pageable)
                    .map(PitchEntityMapper::toDomain);
        }
        return jpaRepository.findByHostId(hostId, pageable)
                .map(PitchEntityMapper::toDomain);
    }

    @Override
    public boolean existsActivePitch(Long creatorId, Long propertyId) {
        return jpaRepository.existsActivePitch(creatorId, propertyId);
    }

    @Override
    public int countByCreatorIdSince(Long creatorId, OffsetDateTime since) {
        return jpaRepository.countByCreatorIdSince(creatorId, since);
    }

    @Override
    public List<CreatorPitch> findExpired(OffsetDateTime now) {
        return jpaRepository.findExpired(now).stream()
                .map(PitchEntityMapper::toDomain)
                .toList();
    }
}
