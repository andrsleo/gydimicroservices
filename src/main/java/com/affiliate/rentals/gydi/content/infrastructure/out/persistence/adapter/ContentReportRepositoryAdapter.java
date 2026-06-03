package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.content.application.port.ContentReportRepositoryPort;
import com.affiliate.rentals.gydi.content.domain.model.ContentReport;
import com.affiliate.rentals.gydi.content.domain.model.ReportStatus;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.mapper.ContentReportEntityMapper;
import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository.ContentReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContentReportRepositoryAdapter implements ContentReportRepositoryPort {

    private final ContentReportJpaRepository jpaRepository;
    private final ContentReportEntityMapper mapper;

    @Override
    public ContentReport save(ContentReport report) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(report)));
    }

    @Override
    public Optional<ContentReport> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<ContentReport> findByStatus(ReportStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status.name(), pageable).map(mapper::toDomain);
    }

    @Override
    public long countDistinctReportersByContentPostId(Long contentPostId) {
        return jpaRepository.countDistinctReportersByContentPostId(contentPostId);
    }

    @Override
    public boolean existsByContentPostIdAndReporterId(Long contentPostId, Long reporterId) {
        return jpaRepository.existsByContentPostIdAndReporterId(contentPostId, reporterId);
    }
}
