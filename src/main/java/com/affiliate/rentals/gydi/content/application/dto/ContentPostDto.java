package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.ContentStatus;
import com.affiliate.rentals.gydi.content.domain.model.ContentType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a content post including its media.
 *
 * @author GYDI Development Team
 */
public record ContentPostDto(
        Long id,
        Long creatorId,
        Long propertyId,
        String caption,
        ContentType type,
        ContentStatus status,
        String thumbnailUrl,
        Integer viewCount,
        Integer likeCount,
        Integer saveCount,
        Integer commentCount,
        Integer shareCount,
        Double engagementScore,
        List<ContentMediaDto> media,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
