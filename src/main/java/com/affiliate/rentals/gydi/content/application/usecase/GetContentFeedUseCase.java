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
 * Use case for retrieving the global content feed ordered by feed_score
 * (time-decayed engagement score) using cursor-based pagination.
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetContentFeedUseCase {

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    /**
     * Executes the use case.
     *
     * @param cursorId the ID of the last post seen in the previous page (null for first page)
     * @param limit    the maximum number of posts to return (capped at 50)
     * @return a cursor-based page of content posts
     */
    public ContentFeedPageDto execute(Long cursorId, int limit) {
        int fetchLimit = Math.min(limit > 0 ? limit : DEFAULT_LIMIT, MAX_LIMIT);
        List<ContentPost> posts = contentPostRepository.findFeedByCursor(cursorId, fetchLimit + 1);
        boolean hasMore = posts.size() > fetchLimit;
        List<ContentPost> page = hasMore ? posts.subList(0, fetchLimit) : posts;
        String nextCursor = hasMore ? String.valueOf(page.getLast().id()) : null;
        List<ContentPostDto> dtos = page.stream().map(mapper::toDto).toList();
        return new ContentFeedPageDto(dtos, nextCursor, hasMore);
    }
}
