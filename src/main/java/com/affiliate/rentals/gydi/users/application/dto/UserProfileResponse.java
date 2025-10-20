package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for user profile response data.
 *
 * <p>This record represents user profile data returned to clients.
 * It uses Java 21 records for immutability and conciseness.</p>
 *
 * @param id the profile's unique identifier
 * @param userId the associated user's ID
 * @param dateOfBirth the user's date of birth
 * @param gender the user's gender
 * @param bio the user's bio/description
 * @param countryCode ISO 3166-1 alpha-3 country code
 * @param timezone IANA timezone identifier
 * @param preferredLanguage ISO 639-1 language code
 * @param avatarUrl URL to user's avatar image
 * @param coverImageUrl URL to user's cover image
 * @param socialLinks map of social media platform to URL/handle
 * @param preferences user preferences as key-value map
 * @param profileVisibility profile visibility level
 * @param emailNotificationsEnabled whether email notifications are enabled
 * @param smsNotificationsEnabled whether SMS notifications are enabled
 * @param metadata extensible metadata map
 * @param createdAt timestamp when profile was created
 * @param updatedAt timestamp when profile was last updated
 * @author GYDI Development Team
 */
public record UserProfileResponse(
        UUID id,
        Long userId,
        LocalDate dateOfBirth,
        String gender,
        String bio,
        String countryCode,
        String timezone,
        String preferredLanguage,
        String avatarUrl,
        String coverImageUrl,
        Map<String, String> socialLinks,
        Map<String, Object> preferences,
        String profileVisibility,
        boolean emailNotificationsEnabled,
        boolean smsNotificationsEnabled,
        Map<String, Object> metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}