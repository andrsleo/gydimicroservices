package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetAllUsersUseCase}.
 *
 * <p>This test class validates the behavior of retrieving all users,
 * including empty results, single users, and multiple users scenarios.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllUsersUseCase Tests")
class GetAllUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoMapper mapper;

    @InjectMocks
    private GetAllUsersUseCase getAllUsersUseCase;

    private User user1;
    private User user2;
    private User user3;
    private UserResponse userResponse1;
    private UserResponse userResponse2;
    private UserResponse userResponse3;

    @BeforeEach
    void setUp() {
        // Setup user 1
        user1 = User.builder()
                .id(1L)
                .email(Email.of("john.doe@example.com"))
                .passwordHash("$2a$10$encodedPassword1")
                .name("John Doe")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.guest()))
                .build();

        userResponse1 = new UserResponse(
                1L,
                "john.doe@example.com",
                "John Doe",
                "+1234567890",
                Set.of("GUEST"),
                LocalDateTime.now()
        );

        // Setup user 2
        user2 = User.builder()
                .id(2L)
                .email(Email.of("jane.smith@example.com"))
                .passwordHash("$2a$10$encodedPassword2")
                .name("Jane Smith")
                .phoneNumber("+1987654321")
                .roles(Set.of(Role.admin()))
                .build();

        userResponse2 = new UserResponse(
                2L,
                "jane.smith@example.com",
                "Jane Smith",
                "+1987654321",
                Set.of("ADMIN"),
                LocalDateTime.now()
        );

        // Setup user 3
        user3 = User.builder()
                .id(3L)
                .email(Email.of("bob.wilson@example.com"))
                .passwordHash("$2a$10$encodedPassword3")
                .name("Bob Wilson")
                .phoneNumber("+1555555555")
                .roles(Set.of(Role.admin(), Role.guest()))
                .build();

        userResponse3 = new UserResponse(
                3L,
                "bob.wilson@example.com",
                "Bob Wilson",
                "+1555555555",
                Set.of("ADMIN", "GUEST"),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userRepository).findAll();
        verify(mapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Should return single user when only one user exists")
    void shouldReturnSingleUserWhenOnlyOneExists() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(mapper.toResponse(user1)).thenReturn(userResponse1);

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).email()).isEqualTo("john.doe@example.com");
        assertThat(result.get(0).name()).isEqualTo("John Doe");

        verify(userRepository).findAll();
        verify(mapper).toResponse(user1);
    }

    @Test
    @DisplayName("Should return all users when multiple users exist")
    void shouldReturnAllUsersWhenMultipleExist() {
        // Arrange
        List<User> users = List.of(user1, user2, user3);
        when(userRepository.findAll()).thenReturn(users);
        when(mapper.toResponse(user1)).thenReturn(userResponse1);
        when(mapper.toResponse(user2)).thenReturn(userResponse2);
        when(mapper.toResponse(user3)).thenReturn(userResponse3);

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // Verify first user
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).email()).isEqualTo("john.doe@example.com");

        // Verify second user
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).email()).isEqualTo("jane.smith@example.com");

        // Verify third user
        assertThat(result.get(2).id()).isEqualTo(3L);
        assertThat(result.get(2).email()).isEqualTo("bob.wilson@example.com");

        verify(userRepository).findAll();
        verify(mapper).toResponse(user1);
        verify(mapper).toResponse(user2);
        verify(mapper).toResponse(user3);
    }

    @Test
    @DisplayName("Should map each user to response DTO")
    void shouldMapEachUserToResponseDto() {
        // Arrange
        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);
        when(mapper.toResponse(user1)).thenReturn(userResponse1);
        when(mapper.toResponse(user2)).thenReturn(userResponse2);

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(userResponse1, userResponse2);

        verify(mapper, times(1)).toResponse(user1);
        verify(mapper, times(1)).toResponse(user2);
    }

    @Test
    @DisplayName("Should preserve user order from repository")
    void shouldPreserveUserOrderFromRepository() {
        // Arrange
        List<User> orderedUsers = List.of(user3, user1, user2);
        when(userRepository.findAll()).thenReturn(orderedUsers);
        when(mapper.toResponse(user3)).thenReturn(userResponse3);
        when(mapper.toResponse(user1)).thenReturn(userResponse1);
        when(mapper.toResponse(user2)).thenReturn(userResponse2);

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(3L); // user3 first
        assertThat(result.get(1).id()).isEqualTo(1L); // user1 second
        assertThat(result.get(2).id()).isEqualTo(2L); // user2 third

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should handle users with different role combinations")
    void shouldHandleUsersWithDifferentRoleCombinations() {
        // Arrange
        User guestUser = User.builder()
                .id(10L)
                .email(Email.of("guest@example.com"))
                .passwordHash("password")
                .name("Guest User")
                .phoneNumber("+1000000000")
                .roles(Set.of(Role.guest()))
                .build();

        User adminUser = User.builder()
                .id(20L)
                .email(Email.of("admin@example.com"))
                .passwordHash("password")
                .name("Admin User")
                .phoneNumber("+2000000000")
                .roles(Set.of(Role.admin()))
                .build();

        User mixedUser = User.builder()
                .id(30L)
                .email(Email.of("mixed@example.com"))
                .passwordHash("password")
                .name("Mixed User")
                .phoneNumber("+3000000000")
                .roles(Set.of(Role.admin(), Role.guest()))
                .build();

        UserResponse guestResponse = new UserResponse(10L, "guest@example.com", "Guest User", "+1000000000", Set.of("GUEST"), LocalDateTime.now());
        UserResponse adminResponse = new UserResponse(20L, "admin@example.com", "Admin User", "+2000000000", Set.of("ADMIN"), LocalDateTime.now());
        UserResponse mixedResponse = new UserResponse(30L, "mixed@example.com", "Mixed User", "+3000000000", Set.of("ADMIN", "GUEST"), LocalDateTime.now());

        when(userRepository.findAll()).thenReturn(List.of(guestUser, adminUser, mixedUser));
        when(mapper.toResponse(guestUser)).thenReturn(guestResponse);
        when(mapper.toResponse(adminUser)).thenReturn(adminResponse);
        when(mapper.toResponse(mixedUser)).thenReturn(mixedResponse);

        // Act
        List<UserResponse> result = getAllUsersUseCase.execute();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).roleNames()).containsExactly("GUEST");
        assertThat(result.get(1).roleNames()).containsExactly("ADMIN");
        assertThat(result.get(2).roleNames()).containsExactlyInAnyOrder("ADMIN", "GUEST");

        verify(userRepository).findAll();
        verify(mapper).toResponse(guestUser);
        verify(mapper).toResponse(adminUser);
        verify(mapper).toResponse(mixedUser);
    }

    @Test
    @DisplayName("Should call repository findAll only once")
    void shouldCallRepositoryFindAllOnlyOnce() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(mapper.toResponse(user1)).thenReturn(userResponse1);

        // Act
        getAllUsersUseCase.execute();

        // Assert
        verify(userRepository, times(1)).findAll();
    }
}
