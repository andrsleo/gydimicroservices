package com.affiliate.rentals.gydi.social.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.social.infrastructure.out.persistence.entity.ShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareJpaRepository extends JpaRepository<ShareEntity, Long> {}
