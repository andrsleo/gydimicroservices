package com.affiliate.rentals.gydi.users.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for user profile response data.
 *
 * <p>
 * This record represents user profile data returned to clients.
 * It uses Java 21 records for immutability and conciseness.
 * </p>
 *
 * @param id                        the profile's unique identifier
 * @param userId                    the associated user's ID
 * @param firstName                 user's first name
 * @param lastName                  user's last name
 * @param dateOfBirth               the user's date of birth
 * @param gender                    the user's gender
 * @param bio                       the user's bio/description
 * @param phoneNumber               user's phone number
 * @param country                   full country name
 * @param city                      user's city
 * @param address                   user's street address
 * @param postalCode                user's postal/ZIP code
 * @param preferredLanguage         ISO 639-1 language code
 * @param coverImageUrl             URL to user's cover image
 * @param websiteUrl                URL to user's personal/business website
 * @param socialLinks               map of social media platform to URL/handle
 * @param preferences               user preferences as key-value map
 * @param profileVisibility         profile visibility level
 * @param emailNotificationsEnabled whether email notifications are enabled
 * @param smsNotificationsEnabled   whether SMS notifications are enabled
 * @param metadata                  extensible metadata map
 * @param createdAt                 timestamp when profile was created
 * @param updatedAt                 timestamp when profile was last updated
 * @author GYDI Development Team
 */
public record UserProfileResponse(
                Long id,
                Long userId,
                String firstName,
                String lastName,
                LocalDate dateOfBirth,
                String gender,
                String bio,
                String phoneNumber,
                String country,
                String city,
                String address,
                String postalCode,
                String preferredLanguage,
                String coverImageUrl,
                String websiteUrl,
                Map<String, String> socialLinks,
                Map<String, Object> preferences,
                String profileVisibility,
                boolean emailNotificationsEnabled,
                boolean smsNotificationsEnabled,
                Map<String, Object> metadata,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}