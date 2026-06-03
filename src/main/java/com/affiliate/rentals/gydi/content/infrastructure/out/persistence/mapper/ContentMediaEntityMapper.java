package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;
import com.affiliate.rentals.gydi.content.domain.model.MediaType;
import com.affiliate.rentals.gydi.content.domain.model.ProcessingStatus;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentMediaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContentMediaEntityMapper {

    public ContentMediaEntity toEntity(ContentMedia domain) {
        ContentMediaEntity entity = new ContentMediaEntity();
        entity.setId(domain.id());
        entity.setContentPostId(domain.contentPostId());
        entity.setOriginalUrl(domain.originalUrl());
        entity.setProcessedUrl(domain.processedUrl());
        entity.setThumbnailUrl(domain.thumbnailUrl());
        entity.setMediaType(domain.mediaType().name());
        entity.setDisplayOrder(domain.displayOrder());
        entity.setDurationSeconds(domain.durationSeconds());
        entity.setWidth(domain.width());
        entity.setHeight(domain.height());
        entity.setFileSizeBytes(domain.fileSizeBytes());
        entity.setProcessingStatus(domain.processingStatus().name());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public ContentMedia toDomain(ContentMediaEntity entity) {
        return ContentMedia.builder()
                .id(entity.getId())
                .contentPostId(entity.getContentPostId())
                .originalUrl(entity.getOriginalUrl())
                .processedUrl(entity.getProcessedUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .mediaType(MediaType.valueOf(entity.getMediaType()))
                .displayOrder(entity.getDisplayOrder())
                .durationSeconds(entity.getDurationSeconds())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .fileSizeBytes(entity.getFileSizeBytes())
                .processingStatus(ProcessingStatus.valueOf(entity.getProcessingStatus()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
