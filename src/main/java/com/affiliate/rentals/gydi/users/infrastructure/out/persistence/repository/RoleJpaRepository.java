package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository interface for RoleEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for the RoleEntity.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to role management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * UserRepositoryAdapter to manage role persistence.</p>
 *
 * @author GYDI Development Team
 * @see RoleEntity
 */
@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    /**
     * Finds a role by its name.
     *
     * @param name the role name (e.g., "ADMIN", "OWNER", "GUEST")
     * @return an Optional containing the role if found, empty otherwise
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * Checks if a role exists with the given name.
     *
     * @param name the role name to check
     * @return true if a role exists with this name, false otherwise
     */
    boolean existsByName(String name);
}
