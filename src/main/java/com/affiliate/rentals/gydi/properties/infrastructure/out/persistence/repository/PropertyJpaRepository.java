package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.affiliate.rentals.gydi.properties.infrastructure.out.persistence.entity.PropertyJpaEntity;

@Repository
public interface PropertyJpaRepository extends JpaRepository<PropertyJpaEntity, Long>,
        JpaSpecificationExecutor<PropertyJpaEntity> {

    List<PropertyJpaEntity> findByHostId(Long hostId);

    Optional<PropertyJpaEntity> findBySlug(String slug);

    List<PropertyJpaEntity> findByStatus(String status);

    /**
     * Finds all properties that have an Airbnb iCal URL configured.
     *
     * @return list of properties with iCal URLs
     */
    List<PropertyJpaEntity> findByIcalUrlAirbnbIsNotNull();

    /**
     * Checks if a property exists with the given Airbnb listing ID.
     *
     * @param airbnbListingId the Airbnb listing ID
     * @return true if exists
     */
    boolean existsByAirbnbListingId(String airbnbListingId);
}
