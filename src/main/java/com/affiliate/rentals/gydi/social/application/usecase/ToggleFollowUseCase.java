package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.shared.events.NewFollowerEvent;
import com.affiliate.rentals.gydi.social.application.port.FollowRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Follow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToggleFollowUseCase {

    private final FollowRepositoryPort followRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean execute(Long followerId, Long followingId) {
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
            log.info("Unfollow: followerId={}, followingId={}", followerId, followingId);
            return false;
        } else {
            followRepository.save(Follow.create(followerId, followingId));
            log.info("Follow: followerId={}, followingId={}", followerId, followingId);
            // Phase 4 — publish event so notifications bounded context can notify the followed user
            eventPublisher.publishEvent(new NewFollowerEvent(followingId, followerId));
            return true;
        }
    }
}
