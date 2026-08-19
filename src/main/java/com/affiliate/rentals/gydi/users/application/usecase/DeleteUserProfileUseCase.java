package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for deleting a user profile.
 *
 * <p>This use case handles the deletion of user profiles.
 * Note: Due to ON DELETE CASCADE in the database, deleting a user will
 * automatically delete their profile.</p>
 *
 * <p><b>SECURITY: IDOR Prevention</b> - Validates that users can only delete their own profiles.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserProfileUseCase {

    private final UserProfileRepositoryPort profileRepository;
    private final OwnershipValidator ownershipValidator;

    /**
     * Deletes a user profile by user ID.
     *
     * @param userId the user ID
     * @throws UserNotFoundException if the profile doesn't exist
     * @throws com.affiliate.rentals.gydi.shared.exception.ForbiddenException if user doesn't own the profile
     */
    @Transactional
    public void execute(Long userId) {
        log.debug("Attempting to delete profile for user ID: {}", userId);

        // SECURITY: Validate ownership to prevent IDOR
        ownershipValidator.validateOwnership(userId);

        if (!profileRepository.existsByUserId(userId)) {
            log.warn("Profile not found for user ID: {}", userId);
            throw new UserNotFoundException("Profile not found for user ID: " + userId);
        }

        profileRepository.deleteByUserId(userId);

        log.info("Successfully deleted profile for user ID: {}", userId);
    }
}