package com.affiliate.rentals.gydi.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link User}.
 *
 * <p>This test class validates the User domain aggregate, including:</p>
 * <ul>
 *   <li>Builder pattern functionality</li>
 *   <li>Required field validation</li>
 *   <li>Default value assignment</li>
 *   <li>Role management (hasRole, isAdmin, addRole, removeRole)</li>
 *   <li>Immutability guarantees</li>
 *   <li>Equality and hashCode behavior</li>
 *   <li>Subscription plan and capabilities</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("User Domain Aggregate Tests")
class UserTest {

    private static final Long USER_ID = 1L;
    private static final String USER_NAME = "John Doe";
    private static final String USER_EMAIL = "john.doe@example.com";
    private static final String PASSWORD_HASH = "$2a$10$hashedPassword";
    private static final String PHONE_NUMBER = "+1234567890";

    // ========== Builder Tests ==========

    @Test
    @DisplayName("Builder should create user with all required fields")
    void builderShouldCreateUserWithAllRequiredFields() {
        // Act
        User user = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .phoneNumber(PHONE_NUMBER)
                .addRole(Role.user())
                .build();

        // Assert
        assertThat(user.id()).isEqualTo(USER_ID);
        assertThat(user.name()).isEqualTo(USER_NAME);
        assertThat(user.email().address()).isEqualTo(USER_EMAIL);
        assertThat(user.passwordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.phoneNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(user.roles()).containsExactly(Role.user());
    }

    @Test
    @DisplayName("Builder should create user with minimal required fields")
    void builderShouldCreateUserWithMinimalRequiredFields() {
        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build();

        // Assert
        assertThat(user.name()).isEqualTo(USER_NAME);
        assertThat(user.email().address()).isEqualTo(USER_EMAIL);
        assertThat(user.passwordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.id()).isNull(); // ID is optional
        assertThat(user.phoneNumber()).isNull(); // Phone is optional
    }

    @Test
    @DisplayName("Builder should allow email to be set as string")
    void builderShouldAllowEmailToBeSetAsString() {
        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(USER_EMAIL) // String instead of Email object
                .passwordHash(PASSWORD_HASH)
                .build();

        // Assert
        assertThat(user.email().address()).isEqualTo(USER_EMAIL);
    }

    @Test
    @DisplayName("Builder should allow adding multiple roles")
    void builderShouldAllowAddingMultipleRoles() {
        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .addRole(Role.admin())
                .build();

        // Assert
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.user(), Role.admin());
    }

    @Test
    @DisplayName("Builder should allow setting roles as a set")
    void builderShouldAllowSettingRolesAsSet() {
        // Arrange
        Set<Role> roles = Set.of(Role.user(), Role.admin());

        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .roles(roles)
                .build();

        // Assert
        assertThat(user.roles()).containsExactlyInAnyOrderElementsOf(roles);
    }

    // ========== Required Field Validation Tests ==========

    @Test
    @DisplayName("Builder should throw NPE when name is null")
    void builderShouldThrowNPEWhenNameIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            User.builder()
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build()
        ).isInstanceOf(NullPointerException.class)
         .hasMessageContaining("Name cannot be null");
    }

    @Test
    @DisplayName("Builder should throw NPE when email is null")
    void builderShouldThrowNPEWhenEmailIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            User.builder()
                .name(USER_NAME)
                .passwordHash(PASSWORD_HASH)
                .build()
        ).isInstanceOf(NullPointerException.class)
         .hasMessageContaining("Email cannot be null");
    }

    @Test
    @DisplayName("Builder should throw NPE when passwordHash is null")
    void builderShouldThrowNPEWhenPasswordHashIsNull() {
        // Act & Assert
        assertThatThrownBy(() ->
            User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .build()
        ).isInstanceOf(NullPointerException.class)
         .hasMessageContaining("Password hash cannot be null");
    }

    // ========== Default Value Tests ==========

    @Test
    @DisplayName("User should have FREE plan by default")
    void userShouldHaveFreePlanByDefault() {
        // Act
        User user = createMinimalUser();

        // Assert
        assertThat(user.activePlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    @DisplayName("User should have default capabilities by default")
    void userShouldHaveDefaultCapabilitiesByDefault() {
        // Act
        User user = createMinimalUser();

        // Assert
        assertThat(user.capabilities()).isEqualTo(UserCapabilities.defaultCapabilities());
        assertThat(user.capabilities().canPublish()).isTrue();
        assertThat(user.capabilities().canRefer()).isTrue();
        assertThat(user.capabilities().canRent()).isTrue();
    }

    @Test
    @DisplayName("User should have accountVerified false by default")
    void userShouldHaveAccountVerifiedFalseByDefault() {
        // Act
        User user = createMinimalUser();

        // Assert
        assertThat(user.isAccountVerified()).isFalse();
    }

    @Test
    @DisplayName("User should have createdAt set to current time by default")
    void userShouldHaveCreatedAtSetToCurrentTimeByDefault() {
        // Arrange
        LocalDateTime before = LocalDateTime.now();

        // Act
        User user = createMinimalUser();

        // Assert
        LocalDateTime after = LocalDateTime.now();
        assertThat(user.createdAt()).isNotNull();
        assertThat(user.createdAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("User should allow custom subscription plan")
    void userShouldAllowCustomSubscriptionPlan() {
        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .activePlan(SubscriptionPlan.ELITE)
                .build();

        // Assert
        assertThat(user.activePlan()).isEqualTo(SubscriptionPlan.ELITE);
    }

    @Test
    @DisplayName("User should allow custom capabilities")
    void userShouldAllowCustomCapabilities() {
        // Arrange
        UserCapabilities customCapabilities = UserCapabilities.of(false, true, false);

        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .capabilities(customCapabilities)
                .build();

        // Assert
        assertThat(user.capabilities()).isEqualTo(customCapabilities);
        assertThat(user.capabilities().canPublish()).isFalse();
        assertThat(user.capabilities().canRefer()).isTrue();
        assertThat(user.capabilities().canRent()).isFalse();
    }

    @Test
    @DisplayName("User should allow custom createdAt timestamp")
    void userShouldAllowCustomCreatedAtTimestamp() {
        // Arrange
        LocalDateTime customTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        // Act
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .createdAt(customTime)
                .build();

        // Assert
        assertThat(user.createdAt()).isEqualTo(customTime);
    }

    // ========== Role Management Tests ==========

    @Test
    @DisplayName("hasRole should return true for USER role")
    void hasRoleShouldReturnTrueForUserRole() {
        // Arrange
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act & Assert
        assertThat(user.hasRole(RoleName.USER)).isTrue();
    }

    @Test
    @DisplayName("hasRole should return true for ADMIN role")
    void hasRoleShouldReturnTrueForAdminRole() {
        // Arrange
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.admin())
                .build();

        // Act & Assert
        assertThat(user.hasRole(RoleName.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("hasRole should return false for role user does not have")
    void hasRoleShouldReturnFalseForRoleUserDoesNotHave() {
        // Arrange
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act & Assert
        assertThat(user.hasRole(RoleName.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("isAdmin should return true for admin user")
    void isAdminShouldReturnTrueForAdminUser() {
        // Arrange
        User adminUser = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.admin())
                .build();

        // Act & Assert
        assertThat(adminUser.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("isAdmin should return false for regular user")
    void isAdminShouldReturnFalseForRegularUser() {
        // Arrange
        User regularUser = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act & Assert
        assertThat(regularUser.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("isAdmin should return true when user has both USER and ADMIN roles")
    void isAdminShouldReturnTrueWhenUserHasBothRoles() {
        // Arrange
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .addRole(Role.admin())
                .build();

        // Act & Assert
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.hasRole(RoleName.USER)).isTrue();
    }

    // ========== Immutability Tests ==========

    @Test
    @DisplayName("addRole should create new user instance with added role")
    void addRoleShouldCreateNewUserInstanceWithAddedRole() {
        // Arrange
        User originalUser = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act
        User modifiedUser = originalUser.addRole(Role.admin());

        // Assert
        assertThat(originalUser.roles()).containsExactly(Role.user()); // Original unchanged
        assertThat(modifiedUser.roles()).containsExactlyInAnyOrder(Role.user(), Role.admin());
        assertThat(modifiedUser.id()).isEqualTo(originalUser.id()); // Other fields preserved
        assertThat(modifiedUser.name()).isEqualTo(originalUser.name());
    }

    @Test
    @DisplayName("removeRole should create new user instance without removed role")
    void removeRoleShouldCreateNewUserInstanceWithoutRemovedRole() {
        // Arrange
        User originalUser = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .addRole(Role.admin())
                .build();

        // Act
        User modifiedUser = originalUser.removeRole(Role.admin());

        // Assert
        assertThat(originalUser.roles()).containsExactlyInAnyOrder(Role.user(), Role.admin()); // Original unchanged
        assertThat(modifiedUser.roles()).containsExactly(Role.user());
        assertThat(modifiedUser.id()).isEqualTo(originalUser.id()); // Other fields preserved
    }

    @Test
    @DisplayName("roles should return unmodifiable set")
    void rolesShouldReturnUnmodifiableSet() {
        // Arrange
        User user = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act & Assert
        assertThatThrownBy(() -> user.roles().add(Role.admin()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========== Equality and HashCode Tests ==========

    @Test
    @DisplayName("Users with same ID and email should be equal")
    void usersWithSameIdAndEmailShouldBeEqual() {
        // Arrange
        User user1 = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build();

        User user2 = User.builder()
                .id(USER_ID)
                .name("Different Name")
                .email(Email.of(USER_EMAIL))
                .passwordHash("differentHash")
                .build();

        // Act & Assert
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("Users with different IDs should not be equal")
    void usersWithDifferentIdsShouldNotBeEqual() {
        // Arrange
        User user1 = User.builder()
                .id(1L)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build();

        User user2 = User.builder()
                .id(2L)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build();

        // Act & Assert
        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("Users with different emails should not be equal")
    void usersWithDifferentEmailsShouldNotBeEqual() {
        // Arrange
        User user1 = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of("user1@example.com"))
                .passwordHash(PASSWORD_HASH)
                .build();

        User user2 = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of("user2@example.com"))
                .passwordHash(PASSWORD_HASH)
                .build();

        // Act & Assert
        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("User should be equal to itself")
    void userShouldBeEqualToItself() {
        // Arrange
        User user = createMinimalUser();

        // Act & Assert
        assertThat(user).isEqualTo(user);
    }

    @Test
    @DisplayName("User should not be equal to null")
    void userShouldNotBeEqualToNull() {
        // Arrange
        User user = createMinimalUser();

        // Act & Assert
        assertThat(user).isNotEqualTo(null);
    }

    @Test
    @DisplayName("User should not be equal to object of different class")
    void userShouldNotBeEqualToObjectOfDifferentClass() {
        // Arrange
        User user = createMinimalUser();
        String notAUser = "not a user";

        // Act & Assert
        assertThat(user).isNotEqualTo(notAUser);
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString should include user details")
    void toStringShouldIncludeUserDetails() {
        // Arrange
        User user = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .build();

        // Act
        String result = user.toString();

        // Assert
        assertThat(result).contains("User");
        assertThat(result).contains(USER_ID.toString());
        assertThat(result).contains(USER_NAME);
        assertThat(result).contains(USER_EMAIL);
    }

    // ========== Integration Tests with Subscription Plan ==========

    @Test
    @DisplayName("Admin user with FREE plan should still have admin privileges")
    void adminUserWithFreePlanShouldStillHaveAdminPrivileges() {
        // Arrange
        User adminUser = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.admin())
                .activePlan(SubscriptionPlan.FREE)
                .build();

        // Act & Assert
        assertThat(adminUser.isAdmin()).isTrue();
        assertThat(adminUser.activePlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    @DisplayName("Verified user with ELITE plan and all capabilities")
    void verifiedUserWithElitePlanAndAllCapabilities() {
        // Arrange
        User eliteUser = User.builder()
                .id(USER_ID)
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .activePlan(SubscriptionPlan.ELITE)
                .capabilities(UserCapabilities.defaultCapabilities())
                .accountVerified(true)
                .build();

        // Act & Assert
        assertThat(eliteUser.activePlan()).isEqualTo(SubscriptionPlan.ELITE);
        assertThat(eliteUser.capabilities().areAllEnabled()).isTrue();
        assertThat(eliteUser.isAccountVerified()).isTrue();
        assertThat(eliteUser.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Unverified user with disabled capabilities")
    void unverifiedUserWithDisabledCapabilities() {
        // Arrange
        UserCapabilities disabledCapabilities = UserCapabilities.allDisabled();
        User restrictedUser = User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .addRole(Role.user())
                .capabilities(disabledCapabilities)
                .accountVerified(false)
                .build();

        // Act & Assert
        assertThat(restrictedUser.isAccountVerified()).isFalse();
        assertThat(restrictedUser.capabilities().areAllDisabled()).isTrue();
        assertThat(restrictedUser.capabilities().canPublish()).isFalse();
        assertThat(restrictedUser.capabilities().canRefer()).isFalse();
        assertThat(restrictedUser.capabilities().canRent()).isFalse();
    }

    // ========== Helper Methods ==========

    private User createMinimalUser() {
        return User.builder()
                .name(USER_NAME)
                .email(Email.of(USER_EMAIL))
                .passwordHash(PASSWORD_HASH)
                .build();
    }
}