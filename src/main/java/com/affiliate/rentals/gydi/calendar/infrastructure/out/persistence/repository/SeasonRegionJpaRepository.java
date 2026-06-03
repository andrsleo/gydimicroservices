package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.SeasonRegionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRegionJpaRepository extends JpaRepository<SeasonRegionJpaEntity, String> {}
