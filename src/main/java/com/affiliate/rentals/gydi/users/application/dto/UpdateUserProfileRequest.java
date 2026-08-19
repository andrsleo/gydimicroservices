package com.affiliate.rentals.gydi.users.application.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for updating an existing user profile.
 *
 * <p>This record encapsulates the data for partial or full profile updates.
 * All fields are optional to support PATCH-style updates.</p>
 *
 * @param firstName user's first name
 * @param lastName user's last name
 * @param dateOfBirth the user's date of birth
 * @param gender the user's gender
 * @param bio user biography/description
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
 * @param profileVisibility profile visibility level
 * @param emailNotificationsEnabled enable email notifications
 * @param smsNotificationsEnabled enable SMS notifications
 * @author GYDI Development Team
 */
public record UpdateUserProfileRequest(
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

        @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format or empty")
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

        @Pattern(regexp = "^$|^https?://.*", message = "Cover image URL must be valid or empty")
        String coverImageUrl,

        @Pattern(regexp = "^$|^https?://.*", message = "Website URL must be valid or empty")
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