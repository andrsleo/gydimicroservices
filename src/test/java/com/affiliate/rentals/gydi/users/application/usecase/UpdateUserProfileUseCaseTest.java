package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UpdateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Gender;
import com.affiliate.rentals.gydi.users.domain.model.ProfileVisibility;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UpdateUserProfileUseCase}.
 *
 * <p>Tests partial update functionality (PATCH semantics) using Java 21 features.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserProfileUseCase Tests")
class UpdateUserProfileUseCaseTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserProfileDtoMapper mapper;

    @InjectMocks
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    private Long userId;
    private UUID profileId;
    private UserProfile existingProfile;

    @BeforeEach
    void setUp() {
        userId = 1L;
        profileId = UUID.randomUUID();

        existingProfile = UserProfile.builder()
                .id(profileId)
                .userId(userId)
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(Gender.MALE)
                .bio("Original bio")
                .countryCode("USA")
                .timezone("America/New_York")
                .preferredLanguage("en")
                .avatarUrl("https://example.com/old-avatar.jpg")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .socialLinks(Map.of("twitter", "@oldhandle"))
                .preferences(Map.of("theme", "light"))
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should update profile with partial data")
    void shouldUpdateProfileWithPartialData() {
        // Arrange
        var updateRequest = new UpdateUserProfileRequest(
                null, // don't update dateOfBirth
                null, // don't update gender
                "Updated bio", // update bio
                null,
                null,
                null,
                "https://example.com/new-avatar.jpg", // update avatar
                null,
                null,
                null,
                null,
                null,
                null
        );

        var updatedProfile = UserProfile.builder()
                .from(existingProfile)
                .bio("Updated bio")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .updatedAt(LocalDateTime.now())
                .build();

        var response = new UserProfileResponse(
                profileId, userId, existingProfile.dateOfBirth(), "male",
                "Updated bio", "USA", "America/New_York", "en",
                "https://example.com/new-avatar.jpg", null,
                Map.of("twitter", "@oldhandle"), Map.of("theme", "light"),
                "public", true, false, Map.of(),
                existingProfile.createdAt(), LocalDateTime.now()
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(updatedProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(response);

        // Act
        var result = updateUserProfileUseCase.execute(userId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.bio()).isEqualTo("Updated bio");
        assertThat(result.avatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
        assertThat(result.gender()).isEqualTo("male"); // unchanged

        verify(profileRepository).findByUserId(userId);
        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when profile doesn't exist")
    void shouldThrowExceptionWhenProfileNotFound() {
        // Arrange
        var updateRequest = new UpdateUserProfileRequest(
                null, null, "New bio", null, null, null, null,
                null, null, null, null, null, null
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateUserProfileUseCase.execute(userId, updateRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(profileRepository).findByUserId(userId);
        verify(profileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should update all fields when all are provided")
    void shouldUpdateAllFieldsWhenProvided() {
        // Arrange
        var updateRequest = new UpdateUserProfileRequest(
                LocalDate.of(1995, 5, 20),
                "female",
                "Completely new bio",
                "CAN",
                "America/Toronto",
                "fr",
                "https://example.com/new-avatar.jpg",
                "https://example.com/new-cover.jpg",
                Map.of("linkedin", "newprofile"),
                Map.of("theme", "dark"),
                "private",
                false,
                true
        );

        var updatedProfile = UserProfile.builder()
                .id(profileId)
                .userId(userId)
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender(Gender.FEMALE)
                .bio("Completely new bio")
                .countryCode("CAN")
                .timezone("America/Toronto")
                .preferredLanguage("fr")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .coverImageUrl("https://example.com/new-cover.jpg")
                .socialLinks(Map.of("linkedin", "newprofile"))
                .preferences(Map.of("theme", "dark"))
                .profileVisibility(ProfileVisibility.PRIVATE)
                .emailNotificationsEnabled(false)
                .smsNotificationsEnabled(true)
                .createdAt(existingProfile.createdAt())
                .updatedAt(LocalDateTime.now())
                .build();

        var response = new UserProfileResponse(
                profileId, userId, LocalDate.of(1995, 5, 20), "female",
                "Completely new bio", "CAN", "America/Toronto", "fr",
                "https://example.com/new-avatar.jpg", "https://example.com/new-cover.jpg",
                Map.of("linkedin", "newprofile"), Map.of("theme", "dark"),
                "private", false, true, Map.of(),
                existingProfile.createdAt(), LocalDateTime.now()
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(updatedProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(response);

        // Act
        var result = updateUserProfileUseCase.execute(userId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 20));
        assertThat(result.gender()).isEqualTo("female");
        assertThat(result.bio()).isEqualTo("Completely new bio");
        assertThat(result.profileVisibility()).isEqualTo("private");

        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should preserve existing values when update fields are null")
    void shouldPreserveExistingValuesWhenNull() {
        // Arrange - all fields null (shouldn't change anything except updatedAt)
        var updateRequest = new UpdateUserProfileRequest(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(existingProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                new UserProfileResponse(
                        profileId, userId, existingProfile.dateOfBirth(), "male",
                        existingProfile.bio(), existingProfile.countryCode(),
                        existingProfile.timezone(), existingProfile.preferredLanguage(),
                        existingProfile.avatarUrl(), existingProfile.coverImageUrl(),
                        existingProfile.socialLinks(), existingProfile.preferences(),
                        "public", existingProfile.emailNotificationsEnabled(),
                        existingProfile.smsNotificationsEnabled(), Map.of(),
                        existingProfile.createdAt(), LocalDateTime.now()
                )
        );

        // Act
        var result = updateUserProfileUseCase.execute(userId, updateRequest);

        // Assert - all original values should be preserved
        assertThat(result).isNotNull();
        assertThat(result.bio()).isEqualTo("Original bio");
        assertThat(result.gender()).isEqualTo("male");
        assertThat(result.avatarUrl()).isEqualTo("https://example.com/old-avatar.jpg");

        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should update only social links")
    void shouldUpdateOnlySocialLinks() {
        // Arrange
        var newSocialLinks = Map.of(
                "github", "github.com/newuser",
                "linkedin", "linkedin.com/in/newuser",
                "website", "https://newuser.dev"
        );

        var updateRequest = new UpdateUserProfileRequest(
                null, null, null, null, null, null, null, null,
                newSocialLinks, // only update social links
                null, null, null, null
        );

        var updatedProfile = UserProfile.builder()
                .from(existingProfile)
                .socialLinks(newSocialLinks)
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(updatedProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                new UserProfileResponse(
                        profileId, userId, existingProfile.dateOfBirth(), "male",
                        existingProfile.bio(), existingProfile.countryCode(),
                        existingProfile.timezone(), existingProfile.preferredLanguage(),
                        existingProfile.avatarUrl(), existingProfile.coverImageUrl(),
                        newSocialLinks, existingProfile.preferences(),
                        "public", true, false, Map.of(),
                        existingProfile.createdAt(), LocalDateTime.now()
                )
        );

        // Act
        var result = updateUserProfileUseCase.execute(userId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.socialLinks()).hasSize(3);
        assertThat(result.socialLinks()).containsKeys("github", "linkedin", "website");

        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should update notification preferences")
    void shouldUpdateNotificationPreferences() {
        // Arrange
        var updateRequest = new UpdateUserProfileRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                false, // disable email notifications
                true   // enable SMS notifications
        );

        var updatedProfile = UserProfile.builder()
                .from(existingProfile)
                .emailNotificationsEnabled(false)
                .smsNotificationsEnabled(true)
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(UserProfile.class))).thenReturn(updatedProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                new UserProfileResponse(
                        profileId, userId, existingProfile.dateOfBirth(), "male",
                        existingProfile.bio(), existingProfile.countryCode(),
                        existingProfile.timezone(), existingProfile.preferredLanguage(),
                        existingProfile.avatarUrl(), existingProfile.coverImageUrl(),
                        existingProfile.socialLinks(), existingProfile.preferences(),
                        "public", false, true, Map.of(),
                        existingProfile.createdAt(), LocalDateTime.now()
                )
        );

        // Act
        var result = updateUserProfileUseCase.execute(userId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.emailNotificationsEnabled()).isFalse();
        assertThat(result.smsNotificationsEnabled()).isTrue();

        verify(profileRepository).save(any(UserProfile.class));
    }
}