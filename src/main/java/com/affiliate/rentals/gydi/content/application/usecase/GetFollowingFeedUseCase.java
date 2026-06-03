package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentFeedPageDto;
import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.social.application.port.FollowRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving the personalized "following" feed for an authenticated user.
 *
 * <p>Returns posts from creators that the viewer follows, ordered by publishedAt DESC,
 * using cursor-based pagination.</p>
 *
 * <p>Cross-context dependency on {@link FollowRepositoryPort} from the social bounded
 * context is intentional — same pattern used by subscriptions using StripeConnectAccount
 * from the commissions context.</p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFollowingFeedUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final FollowRepositoryPort followRepository;
    private final ContentDtoMapper mapper;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    /**
     * Executes the use case.
     *
     * @param viewerId the ID of the authenticated user requesting the feed
     * @param cursorId the ID of the last post seen in the previous page (null for first page)
     * @param limit    the maximum number of posts to return (capped at 50)
     * @return a cursor-based page of content posts from followed creators
     */
    public ContentFeedPageDto execute(Long viewerId, Long cursorId, int limit) {
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(viewerId);
        if (followingIds.isEmpty()) {
            return new ContentFeedPageDto(List.of(), null, false);
        }
        int fetchLimit = Math.min(limit > 0 ? limit : DEFAULT_LIMIT, MAX_LIMIT);
        List<ContentPost> posts = contentPostRepository.findFollowingFeedByCursor(followingIds, cursorId, fetchLimit + 1);
        boolean hasMore = posts.size() > fetchLimit;
        List<ContentPost> page = hasMore ? posts.subList(0, fetchLimit) : posts;
        String nextCursor = hasMore ? String.valueOf(page.getLast().id()) : null;
        List<ContentPostDto> dtos = page.stream().map(mapper::toDto).toList();
        return new ContentFeedPageDto(dtos, nextCursor, hasMore);
    }
}
