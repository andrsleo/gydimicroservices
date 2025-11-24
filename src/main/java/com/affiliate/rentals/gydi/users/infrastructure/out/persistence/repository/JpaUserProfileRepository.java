package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UserProfileEntity.
 *
 * <p>
 * This repository interface provides database access for user profiles
 * using Spring Data JPA's repository abstraction. It extends JpaRepository
 * to inherit common CRUD operations.
 * </p>
 *
 * @author GYDI Development Team
 * @see UserProfileEntity
 */
@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    /**
     * Finds a user profile by the associated user ID.
     *
     * @param userId the user ID
     * @return an Optional containing the profile entity if found
     */
    Optional<UserProfileEntity> findByUserId(Long userId);

    /**
     * Checks if a profile exists for the given user ID.
     *
     * @param userId the user ID
     * @return true if a profile exists, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Deletes a profile by the associated user ID.
     *
     * @param userId the user ID
     */
    void deleteByUserId(Long userId);
}