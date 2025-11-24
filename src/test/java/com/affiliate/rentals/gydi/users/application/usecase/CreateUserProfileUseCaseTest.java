package com.affiliate.rentals.gydi.users.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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
 * <p>
 * This test class follows best practices for unit testing with Java 21:
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
                                userId, // userId
                                "John", // firstName
                                "Doe", // lastName
                                LocalDate.of(1990, 1, 15), // dateOfBirth
                                "male", // gender
                                "Software engineer passionate about clean code", // bio
                                "+15551234567", // phoneNumber
                                "United States", // country
                                "New York", // city
                                "123 Main St", // address
                                "10001", // postalCode
                                "en", // preferredLanguage
                                "https://example.com/cover.jpg", // coverImageUrl
                                "https://johndoe.com", // websiteUrl
                                Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter", "@johndoe"), // socialLinks
                                Map.of("theme", "dark", "notifications", true), // preferences
                                "public", // profileVisibility
                                true, // emailNotificationsEnabled
                                false // smsNotificationsEnabled
                );

                savedProfile = UserProfile.builder()
                                .userId(userId)
                                .firstName("John")
                                .lastName("Doe")
                                .dateOfBirth(LocalDate.of(1990, 1, 15))
                                .gender(Gender.MALE)
                                .bio("Software engineer passionate about clean code")
                                .phoneNumber("+15551234567")
                                .country("United States")
                                .city("New York")
                                .address("123 Main St")
                                .postalCode("10001")
                                .preferredLanguage("en")
                                .coverImageUrl("https://example.com/cover.jpg")
                                .websiteUrl("https://johndoe.com")
                                .socialLinks(Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter",
                                                "@johndoe"))
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
                                "John",
                                "Doe",
                                LocalDate.of(1990, 1, 15),
                                "male",
                                "Software engineer passionate about clean code",
                                "+15551234567",
                                "United States",
                                "New York",
                                "123 Main St",
                                "10001",
                                "en",
                                "https://example.com/cover.jpg",
                                "https://johndoe.com",
                                Map.of("linkedin", "https://linkedin.com/in/johndoe", "twitter", "@johndoe"),
                                Map.of("theme", "dark", "notifications", true),
                                "public",
                                true,
                                false,
                                Map.of(),
                                LocalDateTime.now(),
                                LocalDateTime.now());
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
                                userId, // userId
                                null, // firstName
                                null, // lastName
                                null, // dateOfBirth
                                null, // gender
                                null, // bio
                                null, // phoneNumber
                                null, // country
                                null, // city
                                null, // address
                                null, // postalCode
                                "en", // preferredLanguage
                                null, // coverImageUrl
                                null, // websiteUrl
                                Map.of(), // socialLinks
                                Map.of(), // preferences
                                "public", // profileVisibility
                                true, // emailNotificationsEnabled
                                false // smsNotificationsEnabled
                );

                var minimalProfile = UserProfile.builder()
                                .userId(userId)
                                .build();

                var minimalResponse = new UserProfileResponse(
                                minimalProfile.id(), // id
                                userId, // userId
                                null, // firstName
                                null, // lastName
                                null, // dateOfBirth
                                null, // gender
                                null, // bio
                                null, // phoneNumber
                                null, // country
                                null, // city
                                null, // address
                                null, // postalCode
                                "en", // preferredLanguage
                                null, // coverImageUrl
                                null, // websiteUrl
                                Map.of(), // socialLinks
                                Map.of(), // preferences
                                "public", // profileVisibility
                                true, // emailNotificationsEnabled
                                false, // smsNotificationsEnabled
                                Map.of(), // metadata
                                LocalDateTime.now(), // createdAt
                                LocalDateTime.now() // updatedAt
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
                assertThat(result.preferredLanguage()).isEqualTo("en");
                assertThat(result.profileVisibility()).isEqualTo("public");

                verify(profileRepository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("Should create profile with all gender options")
        void shouldCreateProfileWithAllGenderOptions() {
                // Test all gender values
                var genderValues = new String[] { "male", "female", "non_binary", "prefer_not_to_say", "other" };

                for (int i = 0; i < genderValues.length; i++) {
                        String genderValue = genderValues[i];
                        Long testUserId = 100L + i;

                        // Arrange
                        var requestWithGender = new CreateUserProfileRequest(
                                        testUserId, // userId
                                        null, // firstName
                                        null, // lastName
                                        LocalDate.of(1995, 5, 20), // dateOfBirth
                                        genderValue, // gender
                                        "Test bio", // bio
                                        null, // phoneNumber
                                        null, // country
                                        null, // city
                                        null, // address
                                        null, // postalCode
                                        "en", // preferredLanguage
                                        null, // coverImageUrl
                                        null, // websiteUrl
                                        Map.of(), // socialLinks
                                        Map.of(), // preferences
                                        "public", // profileVisibility
                                        true, // emailNotificationsEnabled
                                        false // smsNotificationsEnabled
                        );

                        var profileWithGender = UserProfile.builder()
                                        .userId(requestWithGender.userId())
                                        .gender(Gender.fromString(genderValue))
                                        .build();

                        when(profileRepository.existsByUserId(any(Long.class))).thenReturn(false);
                        when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(profileWithGender);
                        when(profileRepository.save(any(UserProfile.class))).thenReturn(profileWithGender);
                        when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                                        new UserProfileResponse(
                                                        profileWithGender.id(), // id
                                                        requestWithGender.userId(), // userId
                                                        null, // firstName
                                                        null, // lastName
                                                        LocalDate.of(1995, 5, 20), // dateOfBirth
                                                        genderValue, // gender
                                                        "Test bio", // bio
                                                        null, // phoneNumber
                                                        null, // country
                                                        null, // city
                                                        null, // address
                                                        null, // postalCode
                                                        "en", // preferredLanguage
                                                        null, // coverImageUrl
                                                        null, // websiteUrl
                                                        Map.of(), // socialLinks
                                                        Map.of(), // preferences
                                                        "public", // profileVisibility
                                                        true, // emailNotificationsEnabled
                                                        false, // smsNotificationsEnabled
                                                        Map.of(), // metadata
                                                        LocalDateTime.now(), // createdAt
                                                        LocalDateTime.now() // updatedAt
                                        ));

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
                                "website", "https://user.dev");

                var customPreferences = Map.of(
                                "theme", "dark",
                                "language", "en",
                                "timezone", "America/New_York",
                                "notifications", Map.of(
                                                "email", true,
                                                "push", false,
                                                "sms", false),
                                "privacy", Map.of(
                                                "showEmail", false,
                                                "showPhone", false));

                var customRequest = new CreateUserProfileRequest(
                                userId, // userId
                                "Alex", // firstName
                                "Smith", // lastName
                                LocalDate.of(1992, 6, 10), // dateOfBirth
                                "non_binary", // gender
                                "Full-stack developer", // bio
                                "+1234567890", // phoneNumber
                                "Canada", // country
                                "Toronto", // city
                                "123 Main St", // address
                                "M5H 2N2", // postalCode
                                "en", // preferredLanguage
                                null, // coverImageUrl
                                "https://alexsmith.dev", // websiteUrl
                                customSocialLinks, // socialLinks
                                customPreferences, // preferences
                                "connections", // profileVisibility
                                true, // emailNotificationsEnabled
                                true // smsNotificationsEnabled
                );

                var customProfile = UserProfile.builder()
                                .userId(userId)
                                .socialLinks(customSocialLinks)
                                .preferences(customPreferences)
                                .build();

                when(profileRepository.existsByUserId(userId)).thenReturn(false);
                when(mapper.toDomain(any(CreateUserProfileRequest.class))).thenReturn(customProfile);
                when(profileRepository.save(any(UserProfile.class))).thenReturn(customProfile);
                when(mapper.toResponse(any(UserProfile.class))).thenReturn(
                                new UserProfileResponse(
                                                customProfile.id(), // id
                                                userId, // userId
                                                "Alex", // firstName
                                                "Smith", // lastName
                                                LocalDate.of(1992, 6, 10), // dateOfBirth
                                                "non_binary", // gender
                                                "Full-stack developer", // bio
                                                "+1234567890", // phoneNumber
                                                "Canada", // country
                                                "Toronto", // city
                                                "123 Main St", // address
                                                "M5H 2N2", // postalCode
                                                "en", // preferredLanguage
                                                null, // coverImageUrl
                                                "https://alexsmith.dev", // websiteUrl
                                                customSocialLinks, // socialLinks
                                                customPreferences, // preferences
                                                "connections", // profileVisibility
                                                true, // emailNotificationsEnabled
                                                true, // smsNotificationsEnabled
                                                Map.of(), // metadata
                                                LocalDateTime.now(), // createdAt
                                                LocalDateTime.now() // updatedAt
                                ));

                // Act
                var result = createUserProfileUseCase.execute(customRequest);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.socialLinks()).hasSize(4);
                assertThat(result.preferences()).containsKeys("theme", "language", "notifications", "privacy");

                verify(profileRepository).save(any(UserProfile.class));
        }
}