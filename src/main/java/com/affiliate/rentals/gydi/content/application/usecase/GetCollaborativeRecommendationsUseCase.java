package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for collaborative filtering recommendations.
 *
 * <p>Implements the "users who liked post X also liked post Y" pattern using
 * a SQL self-join on {@code social.likes}. Results are ordered by mutual like
 * count DESC, then feed_score DESC.</p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCollaborativeRecommendationsUseCase {

    private static final int DEFAULT_LIMIT = 10;

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    /**
     * Returns collaboratively filtered recommendations for a given post.
     *
     * @param postId the reference post ID
     * @param userId the authenticated user's ID
     * @return list of recommended posts based on collective user behavior
     */
    public List<ContentPostDto> execute(Long postId, Long userId) {
        return contentPostRepository.findCollaborativeRecommendations(postId, userId, DEFAULT_LIMIT)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}
