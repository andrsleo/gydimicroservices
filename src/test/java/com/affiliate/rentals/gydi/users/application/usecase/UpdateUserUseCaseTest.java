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
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.affiliate.rentals.gydi.users.application.dto.UpdateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;

/**
 * Unit tests for {@link UpdateUserUseCase}.
 *
 * <p>This test class validates the user update functionality,
 * including field updates, role management, and error handling.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserUseCase Tests")
class UpdateUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserDtoMapper mapper;

    @InjectMocks
    private UpdateUserUseCase updateUserUseCase;

    private User existingUser;
    private UpdateUserRequest updateRequest;
    private User updatedUser;
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

        updateRequest = new UpdateUserRequest(
                "John Updated",
                "+1987654321",
                Set.of("ADMIN")
        );

        updatedUser = User.builder()
                .id(EXISTING_USER_ID)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("John Updated")
                .phoneNumber("+1987654321")
                .roles(Set.of(Role.admin()))
                .build();

        expectedResponse = new UserResponse(
                EXISTING_USER_ID,
                "john.doe@example.com",
                "John Updated",
                "+1987654321",
                Set.of("ADMIN"),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should update user successfully with valid data")
    void shouldUpdateUserSuccessfully() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(mapper.toResponse(updatedUser)).thenReturn(expectedResponse);

        // Act
        UserResponse result = updateUserUseCase.execute(EXISTING_USER_ID, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(EXISTING_USER_ID);
        assertThat(result.name()).isEqualTo("John Updated");
        assertThat(result.phoneNumber()).isEqualTo("+1987654321");
        assertThat(result.roleNames()).containsExactly("ADMIN");

        verify(userRepository).findById(EXISTING_USER_ID);
        verify(userRepository).save(any(User.class));
        verify(mapper).toResponse(updatedUser);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateUserUseCase.execute(NON_EXISTING_USER_ID, updateRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTING_USER_ID));

        verify(userRepository).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).save(any(User.class));
        verify(mapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Should preserve email when updating user")
    void shouldPreserveEmailWhenUpdating() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, updateRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.email()).isEqualTo(existingUser.email());
        assertThat(savedUser.email().address()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should preserve password hash when updating user")
    void shouldPreservePasswordHashWhenUpdating() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, updateRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.passwordHash()).isEqualTo(existingUser.passwordHash());
        assertThat(savedUser.passwordHash()).isEqualTo("$2a$10$encodedPassword");
    }

    @Test
    @DisplayName("Should update only name when other fields are null")
    void shouldUpdateOnlyNameWhenOtherFieldsAreNull() {
        // Arrange
        UpdateUserRequest nameOnlyRequest = new UpdateUserRequest(
                "New Name Only",
                null,
                null
        );

        User savedUserWithNewName = User.builder()
                .id(EXISTING_USER_ID)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("New Name Only")
                .phoneNumber(null)
                .roles(existingUser.roles()) // Should preserve existing roles
                .build();

        UserResponse response = new UserResponse(
                EXISTING_USER_ID,
                "john.doe@example.com",
                "New Name Only",
                null,
                Set.of("USER"),
                LocalDateTime.now()
        );

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(savedUserWithNewName);
        when(mapper.toResponse(any(User.class))).thenReturn(response);

        // Act
        UserResponse result = updateUserUseCase.execute(EXISTING_USER_ID, nameOnlyRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Name Only");
        assertThat(result.roleNames()).containsExactly("USER"); // Preserved from existing

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.roles()).isEqualTo(existingUser.roles());
    }

    @Test
    @DisplayName("Should preserve existing roles when roleNames is null")
    void shouldPreserveExistingRolesWhenRoleNamesIsNull() {
        // Arrange
        UpdateUserRequest requestWithoutRoles = new UpdateUserRequest(
                "Updated Name",
                "+1111111111",
                null
        );

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, requestWithoutRoles);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.roles()).isEqualTo(existingUser.roles());
    }

    @Test
    @DisplayName("Should preserve existing roles when roleNames is empty")
    void shouldPreserveExistingRolesWhenRoleNamesIsEmpty() {
        // Arrange
        UpdateUserRequest requestWithEmptyRoles = new UpdateUserRequest(
                "Updated Name",
                "+1111111111",
                Set.of()
        );

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, requestWithEmptyRoles);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.roles()).isEqualTo(existingUser.roles());
    }

    @Test
    @DisplayName("Should update user with multiple roles")
    void shouldUpdateUserWithMultipleRoles() {
        // Arrange
        UpdateUserRequest multiRoleRequest = new UpdateUserRequest(
                "Admin User",
                "+1234567890",
                Set.of("ADMIN", "USER")
        );

        User userWithMultipleRoles = User.builder()
                .id(EXISTING_USER_ID)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword")
                .name("Admin User")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.admin(), Role.user()))
                .build();

        UserResponse multiRoleResponse = new UserResponse(
                EXISTING_USER_ID,
                "john.doe@example.com",
                "Admin User",
                "+1234567890",
                Set.of("ADMIN", "USER"),
                LocalDateTime.now()
        );

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(userWithMultipleRoles);
        when(mapper.toResponse(userWithMultipleRoles)).thenReturn(multiRoleResponse);

        // Act
        UserResponse result = updateUserUseCase.execute(EXISTING_USER_ID, multiRoleRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.roleNames()).containsExactlyInAnyOrder("ADMIN", "USER");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should preserve user ID when updating")
    void shouldPreserveUserIdWhenUpdating() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, updateRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.id()).isEqualTo(EXISTING_USER_ID);
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void shouldCallRepositoryMethodsInCorrectOrder() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(mapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        updateUserUseCase.execute(EXISTING_USER_ID, updateRequest);

        // Assert
        var inOrder = inOrder(userRepository, mapper);
        inOrder.verify(userRepository).findById(EXISTING_USER_ID);
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(mapper).toResponse(any(User.class));
    }
}
