package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentReportJpaRepository extends JpaRepository<ContentReportEntity, Long> {

    Page<ContentReportEntity> findByStatus(String status, Pageable pageable);

    boolean existsByContentPostIdAndReporterId(Long contentPostId, Long reporterId);

    @Query("SELECT COUNT(DISTINCT r.reporterId) FROM ContentReportEntity r WHERE r.contentPostId = :contentPostId")
    long countDistinctReportersByContentPostId(@Param("contentPostId") Long contentPostId);
}
