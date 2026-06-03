package com.affiliate.rentals.gydi.social.application.usecase;

import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.shared.events.ContentLikedEvent;
import com.affiliate.rentals.gydi.social.application.dto.ToggleLikeResponse;
import com.affiliate.rentals.gydi.social.application.port.LikeRepositoryPort;
import com.affiliate.rentals.gydi.social.domain.model.Like;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToggleLikeUseCase {

    private final LikeRepositoryPort likeRepository;
    private final ContentPostRepositoryPort contentPostRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ToggleLikeResponse execute(Long userId, Long contentPostId) {
        var postOpt = contentPostRepository.findById(contentPostId);
        if (postOpt.isEmpty()) return new ToggleLikeResponse(false, 0);

        ContentPost post = postOpt.get();

        if (likeRepository.existsByUserIdAndContentPostId(userId, contentPostId)) {
            likeRepository.deleteByUserIdAndContentPostId(userId, contentPostId);
            post.decrementLikeCount();
            contentPostRepository.save(post);
            return new ToggleLikeResponse(false, post.likeCount());
        } else {
            likeRepository.save(Like.create(userId, contentPostId));
            post.incrementLikeCount();
            contentPostRepository.save(post);
            // Phase 4 — publish event so notifications bounded context can notify creator
            eventPublisher.publishEvent(new ContentLikedEvent(contentPostId, post.creatorId(), userId));
            return new ToggleLikeResponse(true, post.likeCount());
        }
    }
}
