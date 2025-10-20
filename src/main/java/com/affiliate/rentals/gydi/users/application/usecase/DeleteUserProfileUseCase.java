package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for deleting a user profile.
 *
 * <p>This use case handles the deletion of user profiles.
 * Note: Due to ON DELETE CASCADE in the database, deleting a user will
 * automatically delete their profile.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserProfileUseCase {

    private final UserProfileRepository profileRepository;

    /**
     * Deletes a user profile by user ID.
     *
     * @param userId the user ID
     * @throws UserNotFoundException if the profile doesn't exist
     */
    @Transactional
    public void execute(Long userId) {
        log.debug("Attempting to delete profile for user ID: {}", userId);

        if (!profileRepository.existsByUserId(userId)) {
            log.warn("Profile not found for user ID: {}", userId);
            throw new UserNotFoundException("Profile not found for user ID: " + userId);
        }

        profileRepository.deleteByUserId(userId);

        log.info("Successfully deleted profile for user ID: {}", userId);
    }
}