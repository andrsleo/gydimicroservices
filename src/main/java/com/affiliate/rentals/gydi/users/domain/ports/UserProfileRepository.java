package com.affiliate.rentals.gydi.users.domain.ports;

import com.affiliate.rentals.gydi.users.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for UserProfile aggregate.
 *
 * <p>This interface defines the contract for UserProfile persistence operations
 * following the hexagonal architecture pattern. It serves as a port that will be
 * implemented by adapters in the infrastructure layer.</p>
 *
 * @author GYDI Development Team
 * @see UserProfile
 */
public interface UserProfileRepository {

    /**
     * Persists a new user profile or updates an existing one.
     *
     * @param profile the user profile to save
     * @return the saved user profile with generated ID if it was new
     */
    UserProfile save(UserProfile profile);

    /**
     * Finds a user profile by its unique identifier.
     *
     * @param id the profile ID
     * @return an Optional containing the profile if found, empty otherwise
     */
    Optional<UserProfile> findById(UUID id);

    /**
     * Finds a user profile by the associated user ID.
     *
     * @param userId the user ID
     * @return an Optional containing the profile if found, empty otherwise
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * Checks if a profile exists for the given user ID.
     *
     * @param userId the user ID to check
     * @return true if a profile exists, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Deletes a user profile by its ID.
     *
     * @param id the ID of the profile to delete
     */
    void deleteById(UUID id);

    /**
     * Deletes a user profile by the associated user ID.
     *
     * @param userId the user ID
     */
    void deleteByUserId(Long userId);
}