package com.affiliate.rentals.gydi.users.application.mapper;

import com.affiliate.rentals.gydi.users.application.dto.CreateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.domain.model.Gender;
import com.affiliate.rentals.gydi.users.domain.model.ProfileVisibility;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapper for converting between UserProfile domain entities and DTOs.
 *
 * <p>This mapper provides mapping between domain models and application layer DTOs,
 * utilizing Java 21 features for type-safe transformations and working with the
 * immutable UserProfile builder pattern.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class UserProfileDtoMapper {

    /**
     * Maps a UserProfile domain entity to a UserProfileResponse DTO.
     *
     * @param profile the domain user profile entity
     * @return the corresponding UserProfileResponse DTO
     */
    public UserProfileResponse toResponse(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        return new UserProfileResponse(
                profile.id(),
                profile.userId(),
                profile.dateOfBirth(),
                mapGenderToString(profile.gender()),
                profile.bio(),
                profile.countryCode(),
                profile.timezone(),
                profile.preferredLanguage(),
                profile.avatarUrl(),
                profile.coverImageUrl(),
                profile.socialLinks(),
                profile.preferences(),
                mapVisibilityToString(profile.profileVisibility()),
                profile.emailNotificationsEnabled(),
                profile.smsNotificationsEnabled(),
                profile.metadata(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    /**
     * Maps a CreateUserProfileRequest DTO to a UserProfile domain entity.
     *
     * @param request the create request DTO
     * @return the corresponding UserProfile domain entity
     */
    public UserProfile toDomain(CreateUserProfileRequest request) {
        if (request == null) {
            return null;
        }

        return UserProfile.builder()
                .userId(request.userId())
                .dateOfBirth(request.dateOfBirth())
                .gender(mapStringToGender(request.gender()))
                .bio(request.bio())
                .countryCode(request.countryCode())
                .timezone(request.timezone())
                .preferredLanguage(request.preferredLanguage())
                .avatarUrl(request.avatarUrl())
                .coverImageUrl(request.coverImageUrl())
                .socialLinks(request.socialLinks() != null ? request.socialLinks() : Map.of())
                .preferences(request.preferences() != null ? request.preferences() : Map.of())
                .profileVisibility(mapStringToVisibility(request.profileVisibility()))
                .emailNotificationsEnabled(request.emailNotificationsEnabled() != null
                        ? request.emailNotificationsEnabled()
                        : true)
                .smsNotificationsEnabled(request.smsNotificationsEnabled() != null
                        ? request.smsNotificationsEnabled()
                        : false)
                .metadata(Map.of())
                .build();
    }

    /**
     * Converts Gender enum to lowercase string.
     *
     * @param gender the Gender enum
     * @return lowercase string representation or null
     */
    private String mapGenderToString(Gender gender) {
        return gender != null ? gender.toValue() : null;
    }

    /**
     * Converts string to Gender enum.
     *
     * @param gender the gender string
     * @return the Gender enum or null
     */
    private Gender mapStringToGender(String gender) {
        return Gender.fromString(gender);
    }

    /**
     * Converts ProfileVisibility enum to lowercase string.
     *
     * @param visibility the ProfileVisibility enum
     * @return lowercase string representation
     */
    private String mapVisibilityToString(ProfileVisibility visibility) {
        return visibility != null ? visibility.toValue() : null;
    }

    /**
     * Converts string to ProfileVisibility enum.
     *
     * @param visibility the visibility string
     * @return the ProfileVisibility enum
     */
    private ProfileVisibility mapStringToVisibility(String visibility) {
        return ProfileVisibility.fromString(visibility);
    }
}