package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for retrieving a user profile by user ID.
 *
 * <p>This use case retrieves a user profile associated with a specific user,
 * leveraging the 1:1 relationship between User and UserProfile.</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserProfileByUserIdUseCase {

    private final UserProfileRepository profileRepository;
    private final UserProfileDtoMapper mapper;

    /**
     * Retrieves a user profile by user ID.
     *
     * @param userId the user ID
     * @return the profile response
     * @throws UserNotFoundException if the profile is not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse execute(Long userId) {
        log.debug("Retrieving profile for user ID: {}", userId);

        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    log.debug("Found profile for user ID: {}", userId);
                    return mapper.toResponse(profile);
                })
                .orElseThrow(() -> {
                    log.warn("Profile not found for user ID: {}", userId);
                    return new UserNotFoundException("Profile not found for user ID: " + userId);
                });
    }
}