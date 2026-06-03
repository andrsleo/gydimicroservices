package com.affiliate.rentals.gydi.content.application.usecase;

import com.affiliate.rentals.gydi.content.application.dto.ReportContentRequest;
import com.affiliate.rentals.gydi.content.application.port.ContentPostRepositoryPort;
import com.affiliate.rentals.gydi.content.application.port.ContentReportRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.event.ContentReportedEvent;
import com.affiliate.rentals.gydi.content.domain.exception.ContentNotFoundException;
import com.affiliate.rentals.gydi.content.domain.exception.InvalidContentDataException;
import com.affiliate.rentals.gydi.content.domain.model.ContentReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportContentUseCase {

    private static final long AUTO_HIDE_THRESHOLD = 3L;

    private final ContentPostRepositoryPort contentPostRepository;
    private final ContentReportRepositoryPort contentReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long contentId, Long reporterId, ReportContentRequest request) {
        var post = contentPostRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Contenido no encontrado: " + contentId));

        if (contentReportRepository.existsByContentPostIdAndReporterId(contentId, reporterId)) {
            throw new InvalidContentDataException("Ya has reportado este contenido");
        }

        ContentReport report = ContentReport.create(
                contentId,
                reporterId,
                request.reason(),
                request.description()
        );
        contentReportRepository.save(report);

        long distinctReporters = contentReportRepository.countDistinctReportersByContentPostId(contentId);
        if (distinctReporters >= AUTO_HIDE_THRESHOLD) {
            post.hide();
            contentPostRepository.save(post);
            log.warn("Contenido auto-ocultado por reportes: postId={}, reporters={}", contentId, distinctReporters);
        }

        eventPublisher.publishEvent(new ContentReportedEvent(contentId, reporterId, request.reason()));
        log.info("Contenido reportado: postId={}, reporterId={}", contentId, reporterId);
    }
}
