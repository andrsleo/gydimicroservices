package com.affiliate.rentals.gydi.social.application.port;

import com.affiliate.rentals.gydi.social.domain.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FollowRepositoryPort {
    Follow save(Follow follow);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);
    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<Long> findFollowingIdsByFollowerId(Long followerId);
}
