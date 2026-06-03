package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.MediaType;
import com.affiliate.rentals.gydi.content.domain.model.ProcessingStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for a content media file.
 *
 * @author GYDI Development Team
 */
public record ContentMediaDto(
        Long id,
        Long contentPostId,
        String originalUrl,
        String processedUrl,
        String thumbnailUrl,
        MediaType mediaType,
        Integer displayOrder,
        Integer durationSeconds,
        Integer width,
        Integer height,
        Long fileSizeBytes,
        ProcessingStatus processingStatus,
        LocalDateTime createdAt
) {
}
