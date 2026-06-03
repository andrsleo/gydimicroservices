package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.content.domain.model.*;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentPostEntity;
import org.springframework.stereotype.Component;

@Component
public class ContentPostEntityMapper {

    public ContentPostEntity toEntity(ContentPost domain) {
        ContentPostEntity entity = new ContentPostEntity();
        entity.setId(domain.id());
        entity.setCreatorId(domain.creatorId());
        entity.setPropertyId(domain.propertyId());
        entity.setCaption(domain.caption());
        entity.setType(domain.type().name());
        entity.setStatus(domain.status().name());
        entity.setThumbnailUrl(domain.thumbnailUrl());
        entity.setViewCount(domain.viewCount());
        entity.setLikeCount(domain.likeCount());
        entity.setSaveCount(domain.saveCount());
        entity.setCommentCount(domain.commentCount());
        entity.setShareCount(domain.shareCount());
        entity.setEngagementScore(domain.engagementScore());
        entity.setPublishedAt(domain.publishedAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public ContentPost toDomain(ContentPostEntity entity) {
        return ContentPost.builder()
                .id(entity.getId())
                .creatorId(entity.getCreatorId())
                .propertyId(entity.getPropertyId())
                .caption(entity.getCaption())
                .type(ContentType.valueOf(entity.getType()))
                .status(ContentStatus.valueOf(entity.getStatus()))
                .thumbnailUrl(entity.getThumbnailUrl())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .saveCount(entity.getSaveCount())
                .commentCount(entity.getCommentCount())
                .shareCount(entity.getShareCount())
                .engagementScore(entity.getEngagementScore())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
