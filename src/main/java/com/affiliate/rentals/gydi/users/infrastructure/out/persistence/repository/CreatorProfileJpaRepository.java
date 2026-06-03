package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.CreatorProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CreatorProfileJpaRepository extends JpaRepository<CreatorProfileEntity, Long> {

    Page<CreatorProfileEntity> findAll(Pageable pageable);

    @Query("SELECT cp FROM CreatorProfileEntity cp ORDER BY cp.totalViews DESC")
    List<CreatorProfileEntity> findTopByOrderByTotalViewsDesc(Pageable pageable);
}
