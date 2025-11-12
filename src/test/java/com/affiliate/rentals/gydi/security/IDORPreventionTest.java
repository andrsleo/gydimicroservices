package com.affiliate.rentals.gydi.security;

import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.shared.exception.ForbiddenException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for IDOR (Insecure Direct Object Reference) prevention.
 *
 * <p>These tests verify that the OwnershipValidator correctly prevents users
 * from accessing resources they don't own.</p>
 *
 * <p><b>SECURITY: IDOR Prevention Tests</b></p>
 *
 * @author GYDI Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IDOR Prevention Tests")
class IDORPreventionTest {

    @Mock
    private UserRepositoryPort userRepository;

    private OwnershipValidator ownershipValidator;

    private static final Long USER_A_ID = 1L;
    private static final Long USER_B_ID = 2L;
    private static final String USER_A_EMAIL = "userA@example.com";
    private static final String USER_B_EMAIL = "userB@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @BeforeEach
    void setUp() {
        ownershipValidator = new OwnershipValidator(userRepository);

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should allow user to access their own resources")
    void shouldAllowUserToAccessOwnResources() {
        // Given: User A is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When & Then: User A can access their own resource (ID=1)
        ownershipValidator.validateOwnership(USER_A_ID);
        // No exception thrown = success
    }

    @Test
    @DisplayName("Should block user from accessing other user's resources (IDOR attack)")
    void shouldBlockIDORAttack() {
        // Given: User A is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When & Then: User A tries to access User B's resource (ID=2)
        assertThatThrownBy(() -> ownershipValidator.validateOwnership(USER_B_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Should allow ADMIN to access any user's resources")
    void shouldAllowAdminToAccessAnyResource() {
        // Given: ADMIN is authenticated (no repository mock needed - ADMIN bypasses ownership check)
        authenticateUser(ADMIN_EMAIL, "ROLE_ADMIN");

        // When & Then: ADMIN can access any resource
        ownershipValidator.validateOwnership(USER_A_ID);
        ownershipValidator.validateOwnership(USER_B_ID);
        ownershipValidator.validateOwnership(999L);
        // No exceptions thrown = success
    }

    @Test
    @DisplayName("Should throw exception when user is not authenticated")
    void shouldThrowExceptionWhenNotAuthenticated() {
        // Given: No authentication in security context

        // When & Then: Accessing any resource should fail
        assertThatThrownBy(() -> ownershipValidator.validateOwnership(USER_A_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated");
    }

    @Test
    @DisplayName("Should correctly identify ADMIN users")
    void shouldIdentifyAdminUsers() {
        // Given: ADMIN is authenticated
        authenticateUser(ADMIN_EMAIL, "ROLE_ADMIN");

        // When
        boolean isAdmin = ownershipValidator.isAdmin();

        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    @DisplayName("Should correctly identify non-ADMIN users")
    void shouldIdentifyNonAdminUsers() {
        // Given: Regular user is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");

        // When
        boolean isAdmin = ownershipValidator.isAdmin();

        // Then
        assertThat(isAdmin).isFalse();
    }

    @Test
    @DisplayName("Should return authenticated user's ID")
    void shouldReturnAuthenticatedUserId() {
        // Given: User A is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When
        Long userId = ownershipValidator.getAuthenticatedUserId();

        // Then
        assertThat(userId).isEqualTo(USER_A_ID);
    }

    @Test
    @DisplayName("Should return true when user can access resource")
    void shouldReturnTrueWhenCanAccess() {
        // Given: User A is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When
        boolean canAccess = ownershipValidator.canAccess(USER_A_ID);

        // Then
        assertThat(canAccess).isTrue();
    }

    @Test
    @DisplayName("Should return false when user cannot access resource")
    void shouldReturnFalseWhenCannotAccess() {
        // Given: User A is authenticated
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When
        boolean canAccess = ownershipValidator.canAccess(USER_B_ID);

        // Then
        assertThat(canAccess).isFalse();
    }

    /**
     * Test scenario: User A tries to update User B's profile
     * This is the classic IDOR attack scenario
     */
    @Test
    @DisplayName("IDOR Scenario: User A tries to update User B's profile")
    void idorScenario_UpdateOtherUserProfile() {
        // Given: User A is authenticated and tries to update User B's profile
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When & Then: Should throw ForbiddenException
        assertThatThrownBy(() -> {
            // This is what happens in UpdateUserProfileUseCase
            ownershipValidator.validateOwnership(USER_B_ID);
            // ... rest of update logic would not execute
        })
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Access denied");
    }

    /**
     * Test scenario: User A tries to delete User B's account
     */
    @Test
    @DisplayName("IDOR Scenario: User A tries to delete User B's account")
    void idorScenario_DeleteOtherUserAccount() {
        // Given: User A is authenticated and tries to delete User B
        authenticateUser(USER_A_EMAIL, "ROLE_USER");
        mockUserRepository(USER_A_EMAIL, USER_A_ID);

        // When & Then: Should throw ForbiddenException
        assertThatThrownBy(() -> {
            // This is what happens in DeleteUserUseCase
            ownershipValidator.validateOwnership(USER_B_ID);
            // ... rest of delete logic would not execute
        })
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Access denied");
    }

    // ==================== Helper Methods ====================

    private void authenticateUser(String email, String role) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockUserRepository(String email, Long userId) {
        User mockUser = User.builder()
                .id(userId)
                .email(Email.of(email))
                .name("Test User")  // Required field
                .passwordHash("hashed_password")  // Required field
                .roles(Collections.emptySet())  // Required field
                .build();

        when(userRepository.findByEmail(any(Email.class)))
                .thenReturn(Optional.of(mockUser));
    }
}