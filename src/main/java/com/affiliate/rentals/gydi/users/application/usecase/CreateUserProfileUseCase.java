package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.application.dto.CreateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserAlreadyExistsException;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for creating a new user profile.
 *
 * <p>
 * This use case handles the business logic for creating user profiles,
 * ensuring a user doesn't have multiple profiles (1:1 relationship
 * enforcement).
 * </p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserProfileUseCase {

    private final UserProfileRepositoryPort profileRepository;
    private final com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort userRepository;
    private final UserProfileDtoMapper mapper;

    /**
     * Creates a new user profile.
     *
     * @param request the profile creation request
     * @return the created profile response
     * @throws UserAlreadyExistsException if a profile already exists for this user
     */
    @Transactional
    public UserProfileResponse execute(CreateUserProfileRequest request) {
        log.debug("Creating profile for user ID: {}", request.userId());

        // Enforce 1:1 relationship - one profile per user
        if (profileRepository.existsByUserId(request.userId())) {
            log.warn("Profile already exists for user ID: {}", request.userId());
            throw new UserAlreadyExistsException(
                    "Profile already exists for user ID: " + request.userId());
        }

        var profile = mapper.toDomain(request);

        // If phone number is not provided in request, try to get it from User entity
        if (profile.phoneNumber() == null) {
            var user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException(
                            "User not found with ID: " + request.userId()));

            if (user.phoneNumber() != null) {
                profile = profile.withPhoneNumber(user.phoneNumber());
            }
        }

        var savedProfile = profileRepository.save(profile);

        log.info("Successfully created profile with ID: {} for user ID: {}",
                savedProfile.id(), savedProfile.userId());

        return mapper.toResponse(savedProfile);
    }
}