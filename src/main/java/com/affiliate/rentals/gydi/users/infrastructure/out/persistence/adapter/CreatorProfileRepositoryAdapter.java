package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.users.domain.model.CreatorProfile;
import com.affiliate.rentals.gydi.users.domain.model.CreatorTier;
import com.affiliate.rentals.gydi.users.domain.ports.CreatorProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.CreatorProfileEntity;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository.CreatorProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CreatorProfileRepositoryAdapter implements CreatorProfileRepositoryPort {

    private final CreatorProfileJpaRepository jpaRepository;

    @Override
    public CreatorProfile save(CreatorProfile profile) {
        CreatorProfileEntity entity = toEntity(profile);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<CreatorProfile> findByUserId(Long userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Page<CreatorProfile> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<CreatorProfile> findTop(int limit) {
        return jpaRepository.findTopByOrderByTotalViewsDesc(PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    private CreatorProfileEntity toEntity(CreatorProfile p) {
        return new CreatorProfileEntity(p.userId(), p.contentCount(), p.followerCount(),
                p.followingCount(), p.totalViews(), p.avgEngagementRate(),
                p.tier().name(), p.lastContentAt(), p.updatedAt());
    }

    private CreatorProfile toDomain(CreatorProfileEntity e) {
        return CreatorProfile.reconstitute(e.getUserId(), e.getContentCount(), e.getFollowerCount(),
                e.getFollowingCount(), e.getTotalViews(), e.getAvgEngagementRate(),
                CreatorTier.valueOf(e.getTier()), e.getLastContentAt(), e.getUpdatedAt());
    }
}
