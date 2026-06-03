package com.affiliate.rentals.gydi.social.application.port;

import com.affiliate.rentals.gydi.social.domain.model.Like;

import java.util.Optional;

public interface LikeRepositoryPort {
    Like save(Like like);
    Optional<Like> findByUserIdAndContentPostId(Long userId, Long contentPostId);
    void deleteByUserIdAndContentPostId(Long userId, Long contentPostId);
    boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId);
}
