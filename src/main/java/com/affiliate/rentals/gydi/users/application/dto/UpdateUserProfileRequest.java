package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for updating an existing user profile.
 *
 * <p>This record encapsulates the data for partial or full profile updates.
 * All fields are optional to support PATCH-style updates.</p>
 *
 * @param dateOfBirth the user's date of birth
 * @param gender the user's gender
 * @param bio user biography/description
 * @param countryCode ISO 3166-1 alpha-3 country code
 * @param timezone IANA timezone identifier
 * @param preferredLanguage ISO 639-1 language code
 * @param avatarUrl URL to avatar image
 * @param coverImageUrl URL to cover image
 * @param socialLinks map of social media links
 * @param preferences user preferences
 * @param profileVisibility profile visibility level
 * @param emailNotificationsEnabled enable email notifications
 * @param smsNotificationsEnabled enable SMS notifications
 * @author GYDI Development Team
 */
public record UpdateUserProfileRequest(
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Pattern(regexp = "male|female|non_binary|prefer_not_to_say|other",
                message = "Gender must be one of: male, female, non_binary, prefer_not_to_say, other")
        String gender,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,

        @Size(max = 3, message = "Country code must be 3 characters")
        String countryCode,

        String timezone,

        @Size(max = 5, message = "Language code must not exceed 5 characters")
        String preferredLanguage,

        @URL(message = "Avatar URL must be valid")
        String avatarUrl,

        @URL(message = "Cover image URL must be valid")
        String coverImageUrl,

        Map<String, String> socialLinks,

        Map<String, Object> preferences,

        @Pattern(regexp = "public|private|connections",
                message = "Profile visibility must be one of: public, private, connections")
        String profileVisibility,

        Boolean emailNotificationsEnabled,

        Boolean smsNotificationsEnabled
) {
}