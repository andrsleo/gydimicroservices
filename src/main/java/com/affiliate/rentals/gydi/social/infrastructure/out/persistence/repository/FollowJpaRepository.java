package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.FollowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowJpaRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Page<FollowEntity> findByFollowingId(Long followingId, Pageable pageable);
    Page<FollowEntity> findByFollowerId(Long followerId, Pageable pageable);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("SELECT f.followingId FROM FollowEntity f WHERE f.followerId = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);
}
