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
        entity.setFirstName(emptyToNull(profile.firstName()));
        entity.setLastName(emptyToNull(profile.lastName()));
        entity.setDateOfBirth(profile.dateOfBirth());
        entity.setGender(mapGenderToString(profile.gender()));
        entity.setBio(emptyToNull(profile.bio()));
        entity.setPhoneNumber(emptyToNull(profile.phoneNumber()));
        entity.setCountry(emptyToNull(profile.country()));
        entity.setCity(emptyToNull(profile.city()));
        entity.setAddress(emptyToNull(profile.address()));
        entity.setPostalCode(emptyToNull(profile.postalCode()));
        entity.setPreferredLanguage(emptyToNull(profile.preferredLanguage()));
        entity.setCoverImageUrl(emptyToNull(profile.coverImageUrl()));
        entity.setWebsiteUrl(emptyToNull(profile.websiteUrl()));
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
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(mapStringToGender(entity.getGender()))
                .bio(entity.getBio())
                .phoneNumber(entity.getPhoneNumber())
                .country(entity.getCountry())
                .city(entity.getCity())
                .address(entity.getAddress())
                .postalCode(entity.getPostalCode())
                .preferredLanguage(entity.getPreferredLanguage())
                .coverImageUrl(entity.getCoverImageUrl())
                .websiteUrl(entity.getWebsiteUrl())
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

    /**
     * Converts empty strings to null to comply with database check constraints.
     *
     * <p>This is necessary because database constraints like check_website_url_format
     * only allow NULL or valid URLs (starting with http://), but frontend forms may
     * send empty strings which violate the constraint.</p>
     *
     * @param value the string value
     * @return null if the string is null or empty/blank, otherwise the original value
     */
    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}