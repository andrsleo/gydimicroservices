package com.affiliate.rentals.gydi.users.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.affiliate.rentals.gydi.users.application.dto.CreateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserProfileDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserAlreadyExistsException;
import com.affiliate.rentals.gydi.users.domain.model.Gender;
import com.affiliate.rentals.gydi.users.domain.model.ProfileVisibility;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;

/**
 * Unit tests for {@link CreateUserProfileUseCase}.
 *
 * <p>This test class follows best practices for unit testing with Java 21:
 * - Uses Mockito for mocking dependencies
 * - Tests both success and failure scenarios
 * - Uses AssertJ for fluent assertions
 * - Follows AAA pattern (Arrange, Act, Assert)
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserProfileUseCase Tests")
class CreateUserProfileUseCaseTest {

    @Mock
    private UserProfileRepositoryPort profileRepository;

    @Mock
    private UserProfileDtoMapper mapper;

    @InjectMocks
    private CreateUserProfileUseCase createUserProfileUseCase;

    private Long userId;
    private CreateUserProfileRequest validRequest;
    private UserProfile savedProfile;
    private UserProfileResponse expectedResponse;

    @BeforeEach
    void setUp() {
        userId = 1L;

        validRequest = new CreateUserProfileRequest(
                userId,
                LocalDate.of(1990, 1, 15),
                "male",
                "Software engineer passionate about clean code",
                "USA",
                "America/New_York",
                "en",
                "https://example.com/avatar.jpg",
                "https://example.com/cover.jpg",
                Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter", "@johndoe"),
                Map.of("theme", "dark", "notifications", true),
                "public",
                true,
                false
        );

        savedProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(Gender.MALE)
                .bio("Software engineer passionate about clean code")
                .countryCode("USA")
                .timezone("America/New_York")
                .preferredLanguage("en")
                .avatarUrl("https://example.com/avatar.jpg")
                .coverImageUrl("https://example.com/cover.jpg")
                .socialLinks(Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter", "@johndoe"))
                .preferences(Map.of("theme", "dark", "notifications", true))
                .profileVisibility(ProfileVisibility.PUBLIC)
                .emailNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        expectedResponse = new UserProfileResponse(
                savedProfile.id(),
                userId,
                LocalDate.of(1990, 1, 15),
                "male",
                "Software engineer passionate about clean code",
                "USA",
                "America/New_York",
                "en",
                "https://example.com/avatar.jpg",
                "https://example.com/cover.jpg",
                Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter", "@johndoe"),
                Map.of("theme", "dark", "notifications", true),
                "public",
                true,
                false,
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create user profile successfully with valid data")
    void shouldCreateProfileSuccessfully() {
        // Arrange
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(savedProfile);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(expectedResponse);

        // Act
        var result = createUserProfileUseCase.execute(validRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.gender()).isEqualTo("male");
        assertThat(result.bio()).isEqualTo("Software engineer passionate about clean code");
        assertThat(result.profileVisibility()).isEqualTo("public");

        verify(profileRepository).existsByUserId(userId);
        verify(profileRepository).save(any(UserProfile.class));
        verify(mapper).toDomain(validRequest);
        verify(mapper).toResponse(savedProfile);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when profile already exists for user")
    void shouldThrowExceptionWhenProfileExists() {
        // Arrange
        when(profileRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createUserProfileUseCase.execute(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(userId.toString());

        verify(profileRepository).existsByUserId(userId);
        verify(profileRepository, never()).save(any(UserProfile.class));
        verify(mapper, never()).toDomain(any(CreateUserProfileRequest.class));
        verify(mapper, never()).toResponse(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should create profile with minimal data")
    void shouldCreateProfileWithMinimalData() {
        // Arrange
        var minimalRequest = new CreateUserProfileRequest(
                userId,
                null, // no date of birth
                null, // no gender
                null, // no bio
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var minimalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();

        var minimalResponse = new UserProfileResponse(
                minimalProfile.id(),
                userId,
                null,
                null,
                null,
                null,
                "UTC",
                "en",
                null,
                null,
                Map.of(),
                Map.of(),
                "public",
                true,
                false,
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(minimalProfile);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(minimalProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(minimalResponse);

        // Act
        var result = createUserProfileUseCase.execute(minimalRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.timezone()).isEqualTo("UTC");
        assertThat(result.preferredLanguage()).isEqualTo("en");
        assertThat(result.profileVisibility()).isEqualTo("public");

        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should create profile with all gender options")
    void shouldCreateProfileWithAllGenderOptions() {
        // Test all gender values
        var genderValues = new String[]{"male", "female", "non_binary", "prefer_not_to_say", "other"};

        for (int i = 0; i < genderValues.length; i++) {
            String genderValue = genderValues[i];
            Long testUserId = 100L + i;

            // Arrange
            var requestWithGender = new CreateUserProfileRequest(
                    testUserId,
                    LocalDate.of(1995, 5, 20),
                    genderValue,
                    "Test bio",
                    null, null, null, null, null, null, null, null, null, null
            );

            var profileWithGender = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .userId(requestWithGender.userId())
                    .gender(Gender.fromString(genderValue))
                    .build();

            when(profileRepository.existsByUserId(any(Long.class))).thenReturn(false);
            when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(profileWithGender);
            when(profileRepository.save(any(UserProfile.class))).thenReturn(profileWithGender);
            when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                    new UserProfileResponse(
                            profileWithGender.id(), requestWithGender.userId(), LocalDate.of(1995, 5, 20),
                            genderValue, "Test bio", null, "UTC", "en", null, null,
                            Map.of(), Map.of(), "public", true, false, Map.of(),
                            LocalDateTime.now(), LocalDateTime.now()
                    )
            );

            // Act
            var result = createUserProfileUseCase.execute(requestWithGender);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.gender()).isEqualTo(genderValue);
        }
    }

    @Test
    @DisplayName("Should create profile with custom social links and preferences")
    void shouldCreateProfileWithCustomData() {
        // Arrange
        var customSocialLinks = Map.of(
                "github", "github.com/user",
                "linkedin", "linkedin.com/in/user",
                "twitter", "@user",
                "website", "https://user.dev"
        );

        var customPreferences = Map.of(
                "theme", "dark",
                "language", "en",
                "timezone", "America/New_York",
                "notifications", Map.of(
                        "email", true,
                        "push", false,
                        "sms", false
                ),
                "privacy", Map.of(
                        "showEmail", false,
                        "showPhone", false
                )
        );

        var customRequest = new CreateUserProfileRequest(
                userId,
                LocalDate.of(1992, 6, 10),
                "non_binary",
                "Full-stack developer",
                "CAN",
                "America/Toronto",
                "en",
                null,
                null,
                customSocialLinks,
                customPreferences,
                "connections",
                true,
                true
        );

        var customProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .socialLinks(customSocialLinks)
                .preferences(customPreferences)
                .build();

        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(customProfile);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(customProfile);
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                new UserProfileResponse(
                        customProfile.id(), userId, LocalDate.of(1992, 6, 10),
                        "non_binary", "Full-stack developer", "CAN", "America/Toronto", "en",
                        null, null, customSocialLinks, customPreferences, "connections",
                        true, true, Map.of(), LocalDateTime.now(), LocalDateTime.now()
                )
        );

        // Act
        var result = createUserProfileUseCase.execute(customRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.socialLinks()).hasSize(4);
        assertThat(result.preferences()).containsKeys("theme", "language", "notifications", "privacy");

        verify(profileRepository).save(any(UserProfile.class));
    }
}