package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for retrieving a user profile by ID.
 *
 * <p>
 * This use case retrieves a specific user profile by its unique identifier.
 * </p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserProfileByIdUseCase {

    private final UserProfileRepositoryPort profileRepository;
    private final UserProfileDtoMapper mapper;

    /**
     * Retrieves a user profile by ID.
     *
     * @param profileId the profile ID
     * @return the profile response
     * @throws UserNotFoundException if the profile is not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse execute(Long profileId) {
        log.debug("Retrieving profile with ID: {}", profileId);

        return profileRepository.findById(profileId)
                .map(profile -> {
                    log.debug("Found profile with ID: {}", profileId);
                    return mapper.toResponse(profile);
                })
                .orElseThrow(() -> {
                    log.warn("Profile not found with ID: {}", profileId);
                    return new UserNotFoundException("Profile not found with ID: " + profileId);
                });
    }
}