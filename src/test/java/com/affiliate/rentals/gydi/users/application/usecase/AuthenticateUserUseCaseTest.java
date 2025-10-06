package com.affiliate.rentals.gydi.users.application.usecase;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.LoginRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.mapper.UserDtoMapper;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidCredentialsException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.RefreshToken;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.RefreshTokenRepository;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepository;
import com.affiliate.rentals.gydi.users.domain.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticateUserUseCase}.
 *
 * <p>This test class validates the user authentication functionality,
 * including credential validation, JWT token generation, and refresh token creation.</p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticateUserUseCase Tests")
class AuthenticateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDtoMapper mapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticateUserUseCase authenticateUserUseCase;

    private User existingUser;
    private LoginRequest validLoginRequest;
    private UserResponse userResponse;
    private UserDetails userDetails;
    private RefreshToken refreshToken;
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_PASSWORD = "SecurePassword123";
    private static final String INVALID_PASSWORD = "WrongPassword";
    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(USER_ID)
                .email(Email.of(VALID_EMAIL))
                .passwordHash("$2a$10$encodedPassword")
                .name("John Doe")
                .phoneNumber("+1234567890")
                .roles(Set.of(Role.guest()))
                .build();

        validLoginRequest = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

        userResponse = new UserResponse(
                USER_ID,
                VALID_EMAIL,
                "John Doe",
                "+1234567890",
                Set.of("GUEST"),
                LocalDateTime.now()
        );

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(VALID_EMAIL)
                .password("$2a$10$encodedPassword")
                .authorities("ROLE_GUEST")
                .build();

        refreshToken = RefreshToken.create(USER_ID, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void shouldAuthenticateUserSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse result = authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.user()).isEqualTo(userResponse);
        assertThat(result.user().email()).isEqualTo(VALID_EMAIL);

        verify(userRepository).findByEmail(any(Email.class));
        verify(passwordEncoder).matches(VALID_PASSWORD, "$2a$10$encodedPassword");
        verify(userDetailsService).loadUserByUsername(VALID_EMAIL);
        verify(jwtService).generateTokenWithUserId(userDetails, USER_ID);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail(any(Email.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateTokenWithUserId(any(), anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest(VALID_EMAIL, INVALID_PASSWORD);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(INVALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(invalidRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail(any(Email.class));
        verify(passwordEncoder).matches(INVALID_PASSWORD, "$2a$10$encodedPassword");
        verify(jwtService, never()).generateTokenWithUserId(any(), anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate JWT token with user ID claim")
    void shouldGenerateJwtTokenWithUserId() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        verify(jwtService).generateTokenWithUserId(userDetails, USER_ID);
        verify(userDetailsService).loadUserByUsername(VALID_EMAIL);
    }

    @Test
    @DisplayName("Should create and save refresh token")
    void shouldCreateAndSaveRefreshToken() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
        assertThat(savedToken.isValid()).isTrue();
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should authenticate user with ADMIN role")
    void shouldAuthenticateUserWithAdminRole() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .email(Email.of("admin@example.com"))
                .passwordHash("$2a$10$adminPassword")
                .name("Admin User")
                .phoneNumber("+1111111111")
                .roles(Set.of(Role.admin()))
                .build();

        LoginRequest adminRequest = new LoginRequest("admin@example.com", "AdminPass123");

        UserResponse adminResponse = new UserResponse(
                2L,
                "admin@example.com",
                "Admin User",
                "+1111111111",
                Set.of("ADMIN"),
                LocalDateTime.now()
        );

        UserDetails adminUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@example.com")
                .password("$2a$10$adminPassword")
                .authorities("ROLE_ADMIN")
                .build();

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("AdminPass123", "$2a$10$adminPassword")).thenReturn(true);
        when(mapper.toResponse(adminUser)).thenReturn(adminResponse);
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminUserDetails);
        when(jwtService.generateTokenWithUserId(adminUserDetails, 2L)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse result = authenticateUserUseCase.execute(adminRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.user().roleNames()).containsExactly("ADMIN");
        verify(jwtService).generateTokenWithUserId(adminUserDetails, 2L);
    }

    @Test
    @DisplayName("Should authenticate user with multiple roles")
    void shouldAuthenticateUserWithMultipleRoles() {
        // Arrange
        User multiRoleUser = User.builder()
                .id(3L)
                .email(Email.of("multi@example.com"))
                .passwordHash("$2a$10$multiPassword")
                .name("Multi Role User")
                .phoneNumber("+2222222222")
                .roles(Set.of(Role.admin(), Role.guest()))
                .build();

        LoginRequest multiRoleRequest = new LoginRequest("multi@example.com", "MultiPass123");

        UserResponse multiRoleResponse = new UserResponse(
                3L,
                "multi@example.com",
                "Multi Role User",
                "+2222222222",
                Set.of("ADMIN", "GUEST"),
                LocalDateTime.now()
        );

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(multiRoleUser));
        when(passwordEncoder.matches("MultiPass123", "$2a$10$multiPassword")).thenReturn(true);
        when(mapper.toResponse(multiRoleUser)).thenReturn(multiRoleResponse);
        when(userDetailsService.loadUserByUsername("multi@example.com")).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(any(), anyLong())).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse result = authenticateUserUseCase.execute(multiRoleRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.user().roleNames()).containsExactlyInAnyOrder("ADMIN", "GUEST");
    }

    @Test
    @DisplayName("Should validate password using password encoder")
    void shouldValidatePasswordUsingPasswordEncoder() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        verify(passwordEncoder).matches(VALID_PASSWORD, existingUser.passwordHash());
    }

    @Test
    @DisplayName("Should call UserDetailsService with correct email")
    void shouldCallUserDetailsServiceWithCorrectEmail() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        verify(userDetailsService).loadUserByUsername(VALID_EMAIL);
        verify(userDetailsService, never()).loadUserByUsername(argThat(email -> !email.equals(VALID_EMAIL)));
    }

    @Test
    @DisplayName("Should return AuthResponse with Bearer token type")
    void shouldReturnAuthResponseWithBearerTokenType() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse result = authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should execute authentication steps in correct order")
    void shouldExecuteAuthenticationStepsInCorrectOrder() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        var inOrder = inOrder(userRepository, passwordEncoder, mapper, userDetailsService, jwtService, refreshTokenRepository);
        inOrder.verify(userRepository).findByEmail(any(Email.class));
        inOrder.verify(passwordEncoder).matches(VALID_PASSWORD, "$2a$10$encodedPassword");
        inOrder.verify(mapper).toResponse(existingUser);
        inOrder.verify(userDetailsService).loadUserByUsername(VALID_EMAIL);
        inOrder.verify(jwtService).generateTokenWithUserId(userDetails, USER_ID);
        inOrder.verify(jwtService).getRefreshExpiration();
        inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should map user to UserResponse DTO")
    void shouldMapUserToUserResponseDto() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse result = authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        verify(mapper).toResponse(existingUser);
        assertThat(result.user()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("Should create refresh token with correct user ID")
    void shouldCreateRefreshTokenWithCorrectUserId() {
        // Arrange
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(VALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(true);
        when(mapper.toResponse(existingUser)).thenReturn(userResponse);
        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(userDetails);
        when(jwtService.generateTokenWithUserId(userDetails, USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        authenticateUserUseCase.execute(validLoginRequest);

        // Assert
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should not call save refresh token when password validation fails")
    void shouldNotSaveRefreshTokenWhenPasswordValidationFails() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest(VALID_EMAIL, INVALID_PASSWORD);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(INVALID_PASSWORD, "$2a$10$encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticateUserUseCase.execute(invalidRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any());
    }
}
