package com.affiliate.rentals.gydi.content.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.content.infrastructure.out.persistence.entity.ContentMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentMediaJpaRepository extends JpaRepository<ContentMediaEntity, Long> {

    List<ContentMediaEntity> findByContentPostIdOrderByDisplayOrderAsc(Long contentPostId);

    long countByContentPostId(Long contentPostId);
}
