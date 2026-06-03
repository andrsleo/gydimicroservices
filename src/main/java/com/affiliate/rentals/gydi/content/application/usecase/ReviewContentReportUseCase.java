package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ReviewReportRequest;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentReportRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.exception.ContentReportNotFoundException;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewContentReportUseCase {

    private final ContentReportRepositoryPort contentReportRepository;
    private final ContentPostRepositoryPort contentPostRepository;

    @Transactional
    public void execute(Long reportId, Long adminId, ReviewReportRequest request) {
        var report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> new ContentReportNotFoundException(reportId));

        ReportStatus newStatus = request.status();
        report.review(adminId, newStatus);
        contentReportRepository.save(report);

        if (newStatus == ReportStatus.ACTIONED) {
            contentPostRepository.findById(report.contentPostId()).ifPresent(post -> {
                post.reject();
                contentPostRepository.save(post);
                log.info("Contenido rechazado por reporte: postId={}", post.id());
            });
        }

        log.info("Reporte revisado: reportId={}, status={}, adminId={}", reportId, newStatus, adminId);
    }
}
