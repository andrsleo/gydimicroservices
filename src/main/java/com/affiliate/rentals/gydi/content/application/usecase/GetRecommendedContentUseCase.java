package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentFeedPageDto;
import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving a personalized content feed for an authenticated user.
 *
 * <p>Recommendations are based on properties the user has previously liked or saved.
 * Posts the user has already liked are excluded to avoid repetition.</p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecommendedContentUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    /**
     * Returns a personalized feed for the given user.
     *
     * @param userId the authenticated user's ID
     * @param limit  maximum number of posts to return (capped at 50)
     * @return cursor-based page of recommended content posts
     */
    public ContentFeedPageDto execute(Long userId, int limit) {
        int fetchLimit = Math.min(limit > 0 ? limit : DEFAULT_LIMIT, MAX_LIMIT);
        List<ContentPost> posts = contentPostRepository.findRecommendedForUser(userId, fetchLimit + 1);
        boolean hasMore = posts.size() > fetchLimit;
        List<ContentPost> page = hasMore ? posts.subList(0, fetchLimit) : posts;
        String nextCursor = hasMore ? String.valueOf(page.getLast().id()) : null;
        List<ContentPostDto> dtos = page.stream().map(mapper::toDto).toList();
        return new ContentFeedPageDto(dtos, nextCursor, hasMore);
    }
}
