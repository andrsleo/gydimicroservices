package com.affiliate.rentals.gydi.content.application.port;

import com.affiliate.rentals.gydi.content.domain.model.ContentReport;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Output port for ContentReport persistence operations.
 *
 * @author GYDI Development Team
 */
public interface ContentReportRepositoryPort {

    ContentReport save(ContentReport report);

    Optional<ContentReport> findById(Long id);

    Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable);

    long countDistinctReportersByContentPostId(Long contentPostId);

    boolean existsByContentPostIdAndReporterId(Long contentPostId, Long reporterId);
}
