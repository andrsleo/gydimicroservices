package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.mapper;

import com.affiliate.rentals.gydi.users.domain.model.Gender;
import com.affiliate.rentals.gydi.users.domain.model.ProfileVisibility;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity.UserProfileEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between UserProfile domain model and UserProfileEntity.
 *
 * <p>This mapper handles the translation between the domain layer and persistence layer,
 * ensuring proper conversion of enums and maintaining data integrity across layers.
 * Uses manual mapping to work with the immutable UserProfile builder pattern.</p>
 *
 * @author GYDI Development Team
 */
@Component
public class UserProfileEntityMapper {

    /**
     * Converts a UserProfile domain model to a UserProfileEntity.
     *
     * @param profile the domain model
     * @return the persistence entity
     */
    public UserProfileEntity toEntity(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        var entity = new UserProfileEntity();
        entity.setId(profile.id());
        entity.setUserId(profile.userId());
        entity.setDateOfBirth(profile.dateOfBirth());
        entity.setGender(mapGenderToString(profile.gender()));
        entity.setBio(profile.bio());
        entity.setCountryCode(profile.countryCode());
        entity.setTimezone(profile.timezone());
        entity.setPreferredLanguage(profile.preferredLanguage());
        entity.setAvatarUrl(profile.avatarUrl());
        entity.setCoverImageUrl(profile.coverImageUrl());
        entity.setSocialLinks(profile.socialLinks());
        entity.setPreferences(profile.preferences());
        entity.setProfileVisibility(mapVisibilityToString(profile.profileVisibility()));
        entity.setEmailNotificationsEnabled(profile.emailNotificationsEnabled());
        entity.setSmsNotificationsEnabled(profile.smsNotificationsEnabled());
        entity.setMetadata(profile.metadata());
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());

        return entity;
    }

    /**
     * Converts a UserProfileEntity to a UserProfile domain model.
     *
     * @param entity the persistence entity
     * @return the domain model
     */
    public UserProfile toDomain(UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserProfile.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(mapStringToGender(entity.getGender()))
                .bio(entity.getBio())
                .countryCode(entity.getCountryCode())
                .timezone(entity.getTimezone())
                .preferredLanguage(entity.getPreferredLanguage())
                .avatarUrl(entity.getAvatarUrl())
                .coverImageUrl(entity.getCoverImageUrl())
                .socialLinks(entity.getSocialLinks())
                .preferences(entity.getPreferences())
                .profileVisibility(mapStringToVisibility(entity.getProfileVisibility()))
                .emailNotificationsEnabled(entity.getEmailNotificationsEnabled())
                .smsNotificationsEnabled(entity.getSmsNotificationsEnabled())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts Gender enum to string.
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
     * Converts ProfileVisibility enum to string.
     *
     * @param visibility the ProfileVisibility enum
     * @return lowercase string representation or null
     */
    private String mapVisibilityToString(ProfileVisibility visibility) {
        return visibility != null ? visibility.toValue() : null;
    }

    /**
     * Converts string to ProfileVisibility enum.
     *
     * @param visibility the visibility string
     * @return the ProfileVisibility enum or null
     */
    private ProfileVisibility mapStringToVisibility(String visibility) {
        return ProfileVisibility.fromString(visibility);
    }
}