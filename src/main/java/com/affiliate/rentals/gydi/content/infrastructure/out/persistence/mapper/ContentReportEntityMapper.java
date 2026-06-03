package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.content.domain.model.ContentReport;
import com.affiliate.rentals.gydi.content.domain.model.ReportReason;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentReportEntity;
import org.springframework.stereotype.Component;

@Component
public class ContentReportEntityMapper {

    public ContentReportEntity toEntity(ContentReport domain) {
        ContentReportEntity entity = new ContentReportEntity();
        entity.setId(domain.id());
        entity.setContentPostId(domain.contentPostId());
        entity.setReporterId(domain.reporterId());
        entity.setReason(domain.reason().name());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status().name());
        entity.setReviewedBy(domain.reviewedBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setReviewedAt(domain.reviewedAt());
        return entity;
    }

    public ContentReport toDomain(ContentReportEntity entity) {
        return ContentReport.builder()
                .id(entity.getId())
                .contentPostId(entity.getContentPostId())
                .reporterId(entity.getReporterId())
                .reason(ReportReason.valueOf(entity.getReason()))
                .description(entity.getDescription())
                .status(ReportStatus.valueOf(entity.getStatus()))
                .reviewedBy(entity.getReviewedBy())
                .createdAt(entity.getCreatedAt())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }
}
