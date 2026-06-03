package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByUserIdAndContentPostId(Long userId, Long contentPostId);
    void deleteByUserIdAndContentPostId(Long userId, Long contentPostId);
    boolean existsByUserIdAndContentPostId(Long userId, Long contentPostId);
}
