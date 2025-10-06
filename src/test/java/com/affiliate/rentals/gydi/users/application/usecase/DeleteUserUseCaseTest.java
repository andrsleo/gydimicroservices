package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteUserUseCase}.
 *
 * <p>This test class validates the user deletion functionality,
 * ensuring proper exception handling and verification of user existence.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteUserUseCase Tests")
class DeleteUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeleteUserUseCase deleteUserUseCase;

    private User existingUser;
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
                .roles(Set.of(Role.guest()))
                .build();
    }

    @Test
    @DisplayName("Should delete user successfully when user exists")
    void shouldDeleteUserSuccessfullyWhenUserExists() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).deleteById(EXISTING_USER_ID);

        // Act & Assert
        assertThatCode(() -> deleteUserUseCase.execute(EXISTING_USER_ID))
                .doesNotThrowAnyException();

        verify(userRepository).findById(EXISTING_USER_ID);
        verify(userRepository).deleteById(EXISTING_USER_ID);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deleteUserUseCase.execute(NON_EXISTING_USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTING_USER_ID));

        verify(userRepository).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should verify user exists before deleting")
    void shouldVerifyUserExistsBeforeDeleting() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));

        // Act
        deleteUserUseCase.execute(EXISTING_USER_ID);

        // Assert
        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).findById(EXISTING_USER_ID);
        inOrder.verify(userRepository).deleteById(EXISTING_USER_ID);
    }

    @Test
    @DisplayName("Should call deleteById with correct ID")
    void shouldCallDeleteByIdWithCorrectId() {
        // Arrange
        Long userId = 42L;
        User user = User.builder()
                .id(userId)
                .email(Email.of("test@example.com"))
                .passwordHash("password")
                .name("Test User")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.guest()))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        deleteUserUseCase.execute(userId);

        // Assert
        verify(userRepository).deleteById(userId);
        verify(userRepository, never()).deleteById(argThat(id -> !id.equals(userId)));
    }

    @Test
    @DisplayName("Should handle deletion of user with multiple roles")
    void shouldHandleDeletionOfUserWithMultipleRoles() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .email(Email.of("admin@example.com"))
                .passwordHash("password")
                .name("Admin User")
                .phoneNumber("+1111111111")
                .roles(Set.of(Role.admin(), Role.guest()))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        // Act & Assert
        assertThatCode(() -> deleteUserUseCase.execute(2L))
                .doesNotThrowAnyException();

        verify(userRepository).findById(2L);
        verify(userRepository).deleteById(2L);
    }

    @Test
    @DisplayName("Should not call deleteById when findById throws exception")
    void shouldNotCallDeleteByIdWhenFindByIdThrows() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> deleteUserUseCase.execute(EXISTING_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(userRepository).findById(EXISTING_USER_ID);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void shouldHandleNullUserIdGracefully() {
        // Arrange
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deleteUserUseCase.execute(null))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(null);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should call repository methods exactly once for successful deletion")
    void shouldCallRepositoryMethodsExactlyOnce() {
        // Arrange
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));

        // Act
        deleteUserUseCase.execute(EXISTING_USER_ID);

        // Assert
        verify(userRepository, times(1)).findById(EXISTING_USER_ID);
        verify(userRepository, times(1)).deleteById(EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }
}
