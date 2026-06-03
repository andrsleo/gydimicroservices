package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByContentPostIdAndStatusNot(Long contentPostId, String status, Pageable pageable);
}
