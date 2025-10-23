package com.affiliate.rentals.gydi.users.application.usecase;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Unit tests for {@link GetUserByIdUseCase}.
 *
 * <p>This test class validates the behavior of retrieving users by ID,
 * ensuring proper exception handling and data mapping.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserByIdUseCase Tests")
class GetUserByIdUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserDtoMapper mapper;

    @InjectMocks
    private GetUserByIdUseCase getUserByIdUseCase;

    private User existingUser;
    private UserResponse expectedResponse;
    private static final Long EXISTING_USER_ID = 1L;
    private static final Long NON_EXISTING_USER_ID = 999L;

    @BeforeEach
    void setUp() {
    existingUser = User.builder()
                .id(EXISTING_USER_ID)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("John Doe")
                .phoneNumber("+1234567890")
        .roles(Set.of(Role.user()))
                .build();

    expectedResponse = new UserResponse(
                EXISTING_USER_ID,
                "john.doe@example.com",
                "John Doe",
                "+1234567890",
        Set.of("USER"),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should return user when user exists")
    void shouldReturnUserWhenUserExists() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(mapper.toResponse(existingUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = getUserByIdUseCase.execute(EXISTING_USER_ID);

        // Assert
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(EXISTING_USER_ID);
    assertThat(result.email()).isEqualTo("john.doe@example.com");
    assertThat(result.name()).isEqualTo("John Doe");
    assertThat(result.phoneNumber()).isEqualTo("+1234567890");
    assertThat(result.roleNames()).containsExactly("USER");

        verify(userRepository).findById(EXISTING_USER_ID);
        verify(mapper).toResponse(existingUser);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getUserByIdUseCase.execute(NON_EXISTING_USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTING_USER_ID));

        verify(userRepository).findById(NON_EXISTING_USER_ID);
        verify(mapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Should return user with multiple roles")
    void shouldReturnUserWithMultipleRoles() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .email(Email.of("admin@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("Admin User")
                .phoneNumber("+1111111111")
                .roles(Set.of(Role.admin(), Role.user()))
                .build();

    UserResponse adminResponse = new UserResponse(
                2L,
                "admin@example.com",
                "Admin User",
                "+1111111111",
        Set.of("ADMIN", "USER"),
                LocalDateTime.now()
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(mapper.toResponse(adminUser)).thenReturn(adminResponse);

        // Act
        UserResponse result = getUserByIdUseCase.execute(2L);

        // Assert
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(2L);
    assertThat(result.roleNames()).containsExactlyInAnyOrder("ADMIN", "USER");

        verify(userRepository).findById(2L);
        verify(mapper).toResponse(adminUser);
    }

    @Test
    @DisplayName("Should handle null ID gracefully")
    void shouldHandleNullIdGracefully() {
        // Arrange
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getUserByIdUseCase.execute(null))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(null);
        verify(mapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Should call repository with correct ID")
    void shouldCallRepositoryWithCorrectId() {
        // Arrange
        Long userId = 42L;
        User user = User.builder()
                .id(userId)
                .email(Email.of("test@example.com"))
                .passwordHash("password")
                .name("Test User")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.user()))
                .build();

        UserResponse response = new UserResponse(
                userId,
                "test@example.com",
                "Test User",
                "+1234567890",
                Set.of("GUEST"),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(response);

        // Act
        UserResponse result = getUserByIdUseCase.execute(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findById(argThat(id -> !id.equals(userId)));
    }

    @Test
    @DisplayName("Should map domain user to response DTO correctly")
    void shouldMapDomainUserToResponseDto() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(mapper.toResponse(existingUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = getUserByIdUseCase.execute(EXISTING_USER_ID);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(mapper).toResponse(existingUser);
    }
}
