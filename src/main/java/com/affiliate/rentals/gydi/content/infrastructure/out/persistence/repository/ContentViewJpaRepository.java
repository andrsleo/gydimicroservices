package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ContentViewJpaRepository extends JpaRepository<ContentViewEntity, Long> {

    @Query("SELECT COUNT(v) > 0 FROM ContentViewEntity v " +
            "WHERE v.contentPostId = :postId " +
            "AND v.viewerId = :viewerId " +
            "AND CAST(v.createdAt AS date) = :date")
    boolean existsByPostAndViewerAndDate(
            @Param("postId") Long postId,
            @Param("viewerId") Long viewerId,
            @Param("date") LocalDate date);
}
