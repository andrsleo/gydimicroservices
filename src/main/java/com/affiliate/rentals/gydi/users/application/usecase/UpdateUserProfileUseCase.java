package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UpdateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Gender;
import com.affiliate.rentals.gydi.users.domain.model.ProfileVisibility;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case for updating a user profile.
 *
 * <p>This use case handles partial updates to user profiles, applying only
 * the fields that are provided in the request (PATCH semantics).</p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateUserProfileUseCase {

    private final UserProfileRepository profileRepository;
    private final UserProfileDtoMapper mapper;

    /**
     * Updates a user profile with the provided data.
     *
     * @param userId the user ID
     * @param request the update request with fields to modify
     * @return the updated profile response
     * @throws UserNotFoundException if the profile is not found
     */
    @Transactional
    public UserProfileResponse execute(Long userId, UpdateUserProfileRequest request) {
        log.debug("Updating profile for user ID: {}", userId);

        var existingProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Profile not found for user ID: {}", userId);
                    return new UserNotFoundException("Profile not found for user ID: " + userId);
                });

        // Build updated profile using builder pattern with selective updates
        var updatedProfile = buildUpdatedProfile(existingProfile, request);

        var savedProfile = profileRepository.save(updatedProfile);

        log.info("Successfully updated profile for user ID: {}", userId);

        return mapper.toResponse(savedProfile);
    }

    /**
     * Builds an updated UserProfile by applying non-null fields from the request.
     * Uses Java 21 pattern matching and the builder pattern for immutability.
     *
     * @param existing the existing profile
     * @param request the update request
     * @return the updated profile
     */
    private UserProfile buildUpdatedProfile(UserProfile existing, UpdateUserProfileRequest request) {
        var builder = UserProfile.builder()
                .id(existing.id())
                .userId(existing.userId())
                .createdAt(existing.createdAt())
                .updatedAt(LocalDateTime.now());

        // Apply updates only for non-null fields (PATCH semantics)
        builder.dateOfBirth(request.dateOfBirth() != null ? request.dateOfBirth() : existing.dateOfBirth());
        builder.gender(request.gender() != null ? Gender.fromString(request.gender()) : existing.gender());
        builder.bio(request.bio() != null ? request.bio() : existing.bio());
        builder.countryCode(request.countryCode() != null ? request.countryCode() : existing.countryCode());
        builder.timezone(request.timezone() != null ? request.timezone() : existing.timezone());
        builder.preferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : existing.preferredLanguage());
        builder.avatarUrl(request.avatarUrl() != null ? request.avatarUrl() : existing.avatarUrl());
        builder.coverImageUrl(request.coverImageUrl() != null ? request.coverImageUrl() : existing.coverImageUrl());
        builder.socialLinks(request.socialLinks() != null ? request.socialLinks() : existing.socialLinks());
        builder.preferences(request.preferences() != null ? request.preferences() : existing.preferences());
        builder.profileVisibility(request.profileVisibility() != null
                ? ProfileVisibility.fromString(request.profileVisibility())
                : existing.profileVisibility());
        builder.emailNotificationsEnabled(request.emailNotificationsEnabled() != null
                ? request.emailNotificationsEnabled()
                : existing.emailNotificationsEnabled());
        builder.smsNotificationsEnabled(request.smsNotificationsEnabled() != null
                ? request.smsNotificationsEnabled()
                : existing.smsNotificationsEnabled());
        builder.metadata(existing.metadata()); // Metadata preserved unless explicitly updated

        return builder.build();
    }
}