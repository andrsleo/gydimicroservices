package com.affiliate.rentals.gydi.properties.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Amenity Entity
 * Handles database operations for amenities
 */
@Repository
public interface AmenityJpaRepository extends JpaRepository<AmenityJpaEntity, Integer> {

    /**
     * Find amenity by name
     *
     * @param name amenity name
     * @return Optional containing the amenity if found
     */
    Optional<AmenityJpaEntity> findByName(String name);

    /**
     * Find all amenities by category
     *
     * @param category amenity category
     * @return List of amenities in the category
     */
    List<AmenityJpaEntity> findByCategory(String category);

    /**
     * Find amenities by name in list
     *
     * @param names list of amenity names
     * @return List of amenities matching the names
     */
    List<AmenityJpaEntity> findByNameIn(List<String> names);

    /**
     * Check if amenity exists by name
     *
     * @param name amenity name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}