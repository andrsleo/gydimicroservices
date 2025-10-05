package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for UserEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for the UserEntity.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to user management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * UserRepositoryAdapter to implement the UserRepository port.</p>
 *
 * @author GYDI Development Team
 * @see UserEntity
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email address
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email the email to check
     * @return true if a user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users that have a specific role.
     *
     * @param roleName the name of the role to filter by
     * @return a list of users with the specified role
     */
    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);

    /**
     * Finds all users with their roles eagerly loaded.
     *
     * @return a list of all users with roles
     */
    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles")
    List<UserEntity> findAllWithRoles();
}
