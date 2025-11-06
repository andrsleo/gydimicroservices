package com.affiliate.rentals.gydi.users.application.usecase;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.affiliate.rentals.gydi.users.application.dto.CreateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserAlreadyExistsException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.RoleName;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.model.UserProfile;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserProfileRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;

/**
 * Unit tests for {@link CreateUserUseCase}.
 *
 * <p>This test class follows best practices for unit testing:
 * - Uses Mockito for mocking dependencies
 * - Tests both success and failure scenarios
 * - Uses AssertJ for fluent assertions
 * - Follows AAA pattern (Arrange, Act, Assert)
 * - Uses Java 21 features where applicable
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserUseCase Tests")
class CreateUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserProfileRepositoryPort userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDtoMapper mapper;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    private CreateUserRequest validRequest;
    private User savedUser;
    private UserResponse expectedResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequest(
                "john.doe@example.com",
                "SecurePassword123",
                "John",            // firstName
                "Doe",             // lastName
                null,              // name (deprecated)
                "+1234567890",
                Set.of("USER")
        );

        savedUser = User.builder()
                .id(1L)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("John Doe")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.user()))
                .build();

        expectedResponse = new UserResponse(
                1L,
                "john.doe@example.com",
                "John Doe",
                "+1234567890",
                Set.of("USER"),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void shouldCreateUserSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        UserResponse result = createUserUseCase.execute(validRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.phoneNumber()).isEqualTo("+1234567890");
        assertThat(result.roleNames()).containsExactly("USER");

        verify(userRepository).existsByEmail(any(Email.class));
        verify(passwordEncoder).encode("SecurePassword123");
        verify(userRepository).save(any(User.class));
        verify(mapper).toResponse(savedUser);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("john.doe@example.com");

        verify(userRepository).existsByEmail(any(Email.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(mapper, never()).toResponse(any(User.class));
    }

    @Test
        @DisplayName("Should create user with default USER role when no roles provided")
        void shouldCreateUserWithDefaultGuestRole() {
        // Arrange
        CreateUserRequest requestWithoutRoles = new CreateUserRequest(
                "jane.doe@example.com",
                "Password123",
                "Jane",            // firstName
                "Doe",             // lastName
                null,              // name (deprecated)
                "+1987654321",
                null               // roleNames
        );

        User userWithGuestRole = User.builder()
                .id(2L)
                .email(Email.of("jane.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("Jane Doe")
                .phoneNumber("+1987654321")
                .roles(Set.of(Role.user()))
                .build();

        UserResponse response = new UserResponse(
                2L,
                "jane.doe@example.com",
                "Jane Doe",
                "+1987654321",
                Set.of("USER"),
                LocalDateTime.now()
        );

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(userWithGuestRole);
        when(mapper.toResponse(any(User.class))).thenReturn(response);

        // Act
        UserResponse result = createUserUseCase.execute(requestWithoutRoles);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.roleNames()).containsExactly("USER");

        verify(userRepository).existsByEmail(any(Email.class));
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Arrange
        CreateUserRequest adminRequest = new CreateUserRequest(
                "admin@example.com",
                "AdminPass123",
                "Admin",           // firstName
                "User",            // lastName
                null,              // name (deprecated)
                "+1111111111",
                Set.of("USER")
        );

        User adminUser = User.builder()
                .id(3L)
                .email(Email.of("admin@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("Admin User")
                .phoneNumber("+1111111111")
                .roles(Set.of(
                        new Role(1L, RoleName.fromValue("ADMIN")),
                        new Role(2L, RoleName.fromValue("USER"))
                ))
                .build();

        UserResponse adminResponse = new UserResponse(
                3L,
                "admin@example.com",
                "Admin User",
                "+1111111111",
                Set.of("ADMIN", "USER"),
                LocalDateTime.now()
        );

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(mapper.toResponse(any(User.class))).thenReturn(adminResponse);

        // Act
        UserResponse result = createUserUseCase.execute(adminRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.roleNames()).containsExactlyInAnyOrder("ADMIN", "USER");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password before saving user")
    void shouldEncodePasswordBeforeSaving() {
        // Arrange
        String rawPassword = "PlainTextPassword";
        String encodedPassword = "$2a$10$hashedPassword";

        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                rawPassword,
                "Test",            // firstName
                "User",            // lastName
                null,              // name (deprecated)
                "+1234567890",
                Set.of("USER")
        );

        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        createUserUseCase.execute(request);

        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user ->
                user.passwordHash().equals(encodedPassword)
        ));
    }

    @Test
    @DisplayName("Should automatically create UserProfile with defaults when creating a user")
    void shouldCreateUserProfileAutomatically() {
        // Arrange
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        createUserUseCase.execute(validRequest);

        // Assert
        verify(userProfileRepository).save(argThat(profile ->
                profile.userId().equals(savedUser.id()) &&
                profile.preferredLanguage().equals("en") &&
                profile.emailNotificationsEnabled() &&
                !profile.smsNotificationsEnabled()
        ));
    }

    @Test
    @DisplayName("Should not create UserProfile if user creation fails")
    void shouldNotCreateUserProfileIfUserCreationFails() {
        // Arrange
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }
}