package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.mapper.ContentDtoMapper;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving content posts similar to a given post.
 *
 * <p>Similarity is determined by shared property_id or creator_id, ordered by
 * feed_score DESC. This is a public endpoint — no authentication required.</p>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSimilarContentUseCase {

    private static final int DEFAULT_LIMIT = 10;

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentDtoMapper mapper;

    /**
     * Returns up to 10 posts similar to the given post.
     *
     * @param postId the reference post ID
     * @return list of similar content posts as DTOs
     */
    public List<ContentPostDto> execute(Long postId) {
        return contentPostRepository.findSimilarContent(postId, DEFAULT_LIMIT)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}
