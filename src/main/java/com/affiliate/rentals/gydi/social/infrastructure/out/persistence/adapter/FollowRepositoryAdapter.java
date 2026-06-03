package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.social.application.port.FollowRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Follow;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.FollowEntity;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository.FollowJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FollowRepositoryAdapter implements FollowRepositoryPort {

    private final FollowJpaRepository jpaRepository;

    @Override
    public Follow save(Follow follow) {
        FollowEntity entity = new FollowEntity(null, follow.followerId(), follow.followingId(), follow.createdAt());
        FollowEntity saved = jpaRepository.save(entity);
        return Follow.reconstitute(saved.getId(), saved.getFollowerId(), saved.getFollowingId(), saved.getCreatedAt());
    }

    @Override
    public Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        return jpaRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(e -> Follow.reconstitute(e.getId(), e.getFollowerId(), e.getFollowingId(), e.getCreatedAt()));
    }

    @Override
    @Transactional
    public void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        jpaRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public Page<Follow> findByFollowingId(Long followingId, Pageable pageable) {
        return jpaRepository.findByFollowingId(followingId, pageable)
                .map(e -> Follow.reconstitute(e.getId(), e.getFollowerId(), e.getFollowingId(), e.getCreatedAt()));
    }

    @Override
    public Page<Follow> findByFollowerId(Long followerId, Pageable pageable) {
        return jpaRepository.findByFollowerId(followerId, pageable)
                .map(e -> Follow.reconstitute(e.getId(), e.getFollowerId(), e.getFollowingId(), e.getCreatedAt()));
    }

    @Override
    public boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        return jpaRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public List<Long> findFollowingIdsByFollowerId(Long followerId) {
        return jpaRepository.findFollowingIdsByFollowerId(followerId);
    }
}
