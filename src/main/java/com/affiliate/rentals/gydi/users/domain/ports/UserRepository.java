package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for User aggregate.
 *
 * <p>This interface defines the contract for User persistence operations following
 * the hexagonal architecture pattern. It serves as a port that will be implemented
 * by adapters in the infrastructure layer.</p>
 *
 * <p>The repository provides CRUD operations and custom queries for User entities,
 * ensuring the domain layer remains independent of persistence technology.</p>
 *
 * @author GYDI Development Team
 * @see User
 */
public interface UserRepository {

    /**
     * Persists a new user or updates an existing one.
     *
     * @param user the user to save
     * @return the saved user with generated ID if it was new
     */
    User save(User user);

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the user ID
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(Email email);

    /**
     * Retrieves all users in the system.
     *
     * @return a list of all users
     */
    List<User> findAll();

    /**
     * Retrieves all users with a specific role.
     *
     * @param roleName the name of the role to filter by
     * @return a list of users with the specified role
     */
    List<User> findByRole(String roleName);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(Email email);

    /**
     * Deletes a user by their ID.
     *
     * @param id the ID of the user to delete
     */
    void deleteById(Long id);

    /**
     * Counts the total number of users.
     *
     * @return the total number of users
     */
    long count();
}
