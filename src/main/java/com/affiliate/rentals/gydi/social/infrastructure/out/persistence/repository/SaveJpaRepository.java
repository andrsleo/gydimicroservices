package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.SaveEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaveJpaRepository extends JpaRepository<SaveEntity, Long> {
    Optional<SaveEntity> findByUserIdAndContentPostId(Long userId, Long contentPostId);
    void deleteByUserIdAndContentPostId(Long userId, Long contentPostId);
    boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId);
    Page<SaveEntity> findByUserId(Long userId, Pageable pageable);
}
