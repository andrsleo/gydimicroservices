package com.affiliate.rentals.gydi.content.application.dto;

import com.affiliate.rentals.gydi.content.domain.model.ReportReason;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for a content report.
 *
 * @author GYDI Development Team
 */
public record ContentReportDto(
        Long id,
        Long contentPostId,
        Long reporterId,
        ReportReason reason,
        String description,
        ReportStatus status,
        Long reviewedBy,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
