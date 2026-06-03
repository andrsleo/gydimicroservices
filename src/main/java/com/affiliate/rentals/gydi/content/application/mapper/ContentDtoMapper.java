package com.affiliate.rentals.gydi.content.application.mapper;

import com.affiliate.rentals.gydi.content.application.dto.ContentMediaDto;
import com.affiliate.rentals.gydi.content.application.dto.ContentPostDto;
import com.affiliate.rentals.gydi.content.application.dto.ContentReportDto;
import com.affiliate.rentals.gydi.content.domain.model.ContentMedia;
import com.affiliate.rentals.gydi.content.domain.model.ContentPost;
import com.affiliate.rentals.gydi.content.domain.model.ContentReport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mapper for converting between content domain models and application DTOs.
 *
 * @author GYDI Development Team
 */
@Component
public class ContentDtoMapper {

    public ContentPostDto toDto(ContentPost post, List<ContentMedia> media) {
        return new ContentPostDto(
                post.id(),
                post.creatorId(),
                post.propertyId(),
                post.caption(),
                post.type(),
                post.status(),
                post.thumbnailUrl(),
                post.viewCount(),
                post.likeCount(),
                post.saveCount(),
                post.commentCount(),
                post.shareCount(),
                post.engagementScore(),
                media.stream().map(this::toMediaDto).toList(),
                post.publishedAt(),
                post.createdAt(),
                post.updatedAt()
        );
    }

    public ContentPostDto toDto(ContentPost post) {
        return toDto(post, List.of());
    }

    public ContentMediaDto toMediaDto(ContentMedia media) {
        return new ContentMediaDto(
                media.id(),
                media.contentPostId(),
                media.originalUrl(),
                media.processedUrl(),
                media.thumbnailUrl(),
                media.mediaType(),
                media.displayOrder(),
                media.durationSeconds(),
                media.width(),
                media.height(),
                media.fileSizeBytes(),
                media.processingStatus(),
                media.createdAt()
        );
    }

    public ContentReportDto toReportDto(ContentReport report) {
        return new ContentReportDto(
                report.id(),
                report.contentPostId(),
                report.reporterId(),
                report.reason(),
                report.description(),
                report.status(),
                report.reviewedBy(),
                report.createdAt(),
                report.reviewedAt()
        );
    }
}
