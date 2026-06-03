package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.social.application.port.LikeRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Like;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.LikeEntity;
import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository.LikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LikeRepositoryAdapter implements LikeRepositoryPort {

    private final LikeJpaRepository jpaRepository;

    @Override
    public Like save(Like like) {
        LikeEntity entity = new LikeEntity(null, like.userId(), like.contentPostId(), like.createdAt());
        LikeEntity saved = jpaRepository.save(entity);
        return Like.reconstitute(saved.getId(), saved.getUserId(), saved.getContentPostId(), saved.getCreatedAt());
    }

    @Override
    public Optional<Like> findByUserIdAndContentPostId(Long userId, Long contentPostId) {
        return jpaRepository.findByUserIdAndContentPostId(userId, contentPostId)
                .map(e -> Like.reconstitute(e.getId(), e.getUserId(), e.getContentPostId(), e.getCreatedAt()));
    }

    @Override
    @Transactional
    public void deleteByUserIdAndContentPostId(Long userId, Long contentPostId) {
        jpaRepository.deleteByUserIdAndContentPostId(userId, contentPostId);
    }

    @Override
    public boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId) {
        return jpaRepository.existsByUserIdAndContentPostId(userId, contentPostId);
    }
}
