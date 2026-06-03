package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.entity.PropertySeasonPricingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertySeasonPricingJpaRepository extends JpaRepository<PropertySeasonPricingJpaEntity, Long> {

    List<PropertySeasonPricingJpaEntity> findByPropertyId(Long propertyId);

    @Modifying
    @Query("DELETE FROM PropertySeasonPricingJpaEntity p WHERE p.propertyId = :propertyId")
    void deleteByPropertyId(@Param("propertyId") Long propertyId);
}
