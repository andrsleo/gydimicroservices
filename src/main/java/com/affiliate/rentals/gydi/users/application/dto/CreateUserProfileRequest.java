package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for creating a new user profile.
 *
 * <p>This record encapsulates the data required to create a user profile.
 * It includes validation annotations to ensure data integrity at the API boundary.</p>
 *
 * @param userId the associated user's ID (required)
 * @param firstName user's first name
 * @param lastName user's last name
 * @param dateOfBirth the user's date of birth (must be in the past)
 * @param gender the user's gender (male, female, non_binary, prefer_not_to_say, other)
 * @param bio user biography/description (max 1000 characters)
 * @param phoneNumber user's phone number in E.164 format
 * @param country full country name
 * @param city user's city
 * @param address user's street address
 * @param postalCode user's postal/ZIP code
 * @param preferredLanguage ISO 639-1 language code
 * @param coverImageUrl URL to cover image
 * @param websiteUrl URL to personal/business website
 * @param socialLinks map of social media links
 * @param preferences user preferences
 * @param profileVisibility profile visibility (public, private, connections)
 * @param emailNotificationsEnabled enable email notifications
 * @param smsNotificationsEnabled enable SMS notifications
 * @author GYDI Development Team
 */
public record CreateUserProfileRequest(
        Long userId,

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Pattern(regexp = "male|female|non_binary|prefer_not_to_say|other",
                message = "Gender must be one of: male, female, non_binary, prefer_not_to_say, other")
        String gender,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
        String phoneNumber,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @Size(max = 5, message = "Language code must not exceed 5 characters")
        String preferredLanguage,

        @URL(message = "Cover image URL must be valid")
        String coverImageUrl,

        @URL(message = "Website URL must be valid")
        @Pattern(regexp = "^https?://.*", message = "Website URL must start with http:// or https://")
        String websiteUrl,

        Map<String, String> socialLinks,

        Map<String, Object> preferences,

        @Pattern(regexp = "public|private|connections",
                message = "Profile visibility must be one of: public, private, connections")
        String profileVisibility,

        Boolean emailNotificationsEnabled,

        Boolean smsNotificationsEnabled
) {
}