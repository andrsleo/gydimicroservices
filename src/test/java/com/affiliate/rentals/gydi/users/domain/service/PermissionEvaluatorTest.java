package com.affiliate.rentals.gydi.users.domain.service;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.Permission;
import com.affiliate.rentals.gydi.users.domain.model.ResourceType;
import com.affiliate.rentals.gydi.users.domain.model.Role;
import com.affiliate.rentals.gydi.users.domain.model.SubscriptionPlan;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.model.UserCapabilities;

/**
 * Unit tests for {@link PermissionEvaluator}.
 *
 * <p>This test class validates the permission evaluation logic, including:</p>
 * <ul>
 *   <li>Admin override behavior</li>
 *   <li>Account verification requirements</li>
 *   <li>Capability-based permissions</li>
 *   <li>Subscription plan-based permissions</li>
 *   <li>Resource limit enforcement</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("PermissionEvaluator Domain Service Tests")
class PermissionEvaluatorTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "user@example.com";

    @Test
    @DisplayName("Admin should have all permissions")
    void adminShouldHaveAllPermissions() {
        // Arrange
        User admin = createAdminUser();

        // Act & Assert - Admin should have ALL permissions
        assertThat(PermissionEvaluator.evaluate(admin, Permission.PROFILE_READ)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.PROFILE_UPDATE)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.PROPERTY_PUBLISH)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.REFERRAL_GENERATE)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.USER_MANAGE)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.ANALYTICS_VIEW_ADVANCED)).isTrue();
        assertThat(PermissionEvaluator.evaluate(admin, Permission.ANALYTICS_EXPORT)).isTrue();
    }

    @Test
    @DisplayName("Unverified user should not have verification-required permissions")
    void unverifiedUserShouldNotHaveVerificationRequiredPermissions() {
        // Arrange
        User unverifiedUser = User.builder()
                .id(USER_ID)
                .email(Email.of(USER_EMAIL))
                .name("Unverified User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.user()))
                .activePlan(SubscriptionPlan.PRO)
                .capabilities(UserCapabilities.defaultCapabilities())
                .accountVerified(false) // NOT VERIFIED
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(unverifiedUser, Permission.PROPERTY_PUBLISH)).isFalse();
        assertThat(PermissionEvaluator.evaluate(unverifiedUser, Permission.REFERRAL_GENERATE)).isFalse();
    }

    @Test
    @DisplayName("Verified user should have verification-required permissions")
    void verifiedUserShouldHaveVerificationRequiredPermissions() {
        // Arrange
        User verifiedUser = User.builder()
                .id(USER_ID)
                .email(Email.of(USER_EMAIL))
                .name("Verified User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.user()))
                .activePlan(SubscriptionPlan.PRO)
                .capabilities(UserCapabilities.defaultCapabilities())
                .accountVerified(true) // VERIFIED
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(verifiedUser, Permission.PROPERTY_PUBLISH)).isTrue();
        assertThat(PermissionEvaluator.evaluate(verifiedUser, Permission.REFERRAL_GENERATE)).isTrue();
    }

    @Test
    @DisplayName("User with disabled publish capability should not publish")
    void userWithDisabledPublishCapabilityShouldNotPublish() {
        // Arrange
        UserCapabilities disabledPublish = UserCapabilities.of(false, true, true);
        User user = User.builder()
                .id(USER_ID)
                .email(Email.of(USER_EMAIL))
                .name("User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.user()))
                .activePlan(SubscriptionPlan.PRO)
                .capabilities(disabledPublish)
                .accountVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(user, Permission.PROPERTY_PUBLISH)).isFalse();
    }

    @Test
    @DisplayName("User with disabled refer capability should not generate referrals")
    void userWithDisabledReferCapabilityShouldNotGenerateReferrals() {
        // Arrange
        UserCapabilities disabledRefer = UserCapabilities.of(true, false, true);
        User user = User.builder()
                .id(USER_ID)
                .email(Email.of(USER_EMAIL))
                .name("User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.user()))
                .activePlan(SubscriptionPlan.PRO)
                .capabilities(disabledRefer)
                .accountVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(user, Permission.REFERRAL_GENERATE)).isFalse();
    }
    
    @Test
    @DisplayName("FREE plan user should NOT have advanced analytics")
    void freePlanUserShouldNotHaveAdvancedAnalytics() {
        // Arrange
        User freeUser = createVerifiedUserWithPlan(SubscriptionPlan.FREE);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(freeUser, Permission.ANALYTICS_VIEW_ADVANCED)).isFalse();
        assertThat(PermissionEvaluator.evaluate(freeUser, Permission.ANALYTICS_EXPORT)).isFalse();
    }

    @Test
    @DisplayName("PRO plan user should have advanced analytics but not export")
    void proPlanUserShouldHaveAdvancedAnalytics() {
        // Arrange
        User proUser = createVerifiedUserWithPlan(SubscriptionPlan.PRO);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(proUser, Permission.ANALYTICS_VIEW_ADVANCED)).isTrue();
        assertThat(PermissionEvaluator.evaluate(proUser, Permission.ANALYTICS_EXPORT)).isFalse(); // Export only for ELITE
    }

    @Test
    @DisplayName("ELITE plan user should have advanced analytics")
    void elitePlanUserShouldHaveAdvancedAnalytics() {
        // Arrange
        User eliteUser = createVerifiedUserWithPlan(SubscriptionPlan.ELITE);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(eliteUser, Permission.ANALYTICS_VIEW_ADVANCED)).isTrue();
        assertThat(PermissionEvaluator.evaluate(eliteUser, Permission.ANALYTICS_EXPORT)).isTrue();
    }

    @Test
    @DisplayName("Regular user should NOT have admin-only permissions")
    void regularUserShouldNotHaveAdminOnlyPermissions() {
        // Arrange
        User regularUser = createVerifiedUserWithPlan(SubscriptionPlan.ELITE);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(regularUser, Permission.USER_MANAGE)).isFalse();
    }

    @Test
    @DisplayName("All users should have basic read permissions")
    void allUsersShouldHaveBasicReadPermissions() {
        // Arrange
        User user = createVerifiedUserWithPlan(SubscriptionPlan.FREE);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(user, Permission.PROFILE_READ)).isTrue();
        assertThat(PermissionEvaluator.evaluate(user, Permission.PROFILE_UPDATE)).isTrue();
        assertThat(PermissionEvaluator.evaluate(user, Permission.ANALYTICS_VIEW_OWN)).isTrue();
    }

    // ========== Resource Limit Tests ==========

    @Test
    @DisplayName("Admin should never exceed resource limits")
    void adminShouldNeverExceedResourceLimits() {
        // Arrange
        User admin = createAdminUser();

        // Act & Assert - Even with 1000 properties, admin can still create more
        assertThat(PermissionEvaluator.canExceedLimit(admin, ResourceType.PROPERTY, 1000)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(admin, ResourceType.REFERRAL, 1000)).isTrue();
    }

    @Test
    @DisplayName("FREE user should respect 10 property limit")
    void freeUserShouldRespect10PropertyLimit() {
        // Arrange
        User freeUser = createVerifiedUserWithPlan(SubscriptionPlan.FREE);

        // Act & Assert
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.PROPERTY, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.PROPERTY, 5)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.PROPERTY, 9)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.PROPERTY, 10)).isFalse();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.PROPERTY, 11)).isFalse();
    }

    @Test
    @DisplayName("FREE user should respect 50 referral limit")
    void freeUserShouldRespect50ReferralLimit() {
        // Arrange
        User freeUser = createVerifiedUserWithPlan(SubscriptionPlan.FREE);

        // Act & Assert
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.REFERRAL, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.REFERRAL, 25)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.REFERRAL, 49)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.REFERRAL, 50)).isFalse();
        assertThat(PermissionEvaluator.canExceedLimit(freeUser, ResourceType.REFERRAL, 51)).isFalse();
    }

    @Test
    @DisplayName("PRO user should respect 50 property limit")
    void proUserShouldRespect50PropertyLimit() {
        // Arrange
        User proUser = createVerifiedUserWithPlan(SubscriptionPlan.PRO);

        // Act & Assert
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.PROPERTY, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.PROPERTY, 25)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.PROPERTY, 49)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.PROPERTY, 50)).isFalse();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.PROPERTY, 51)).isFalse();
    }

    @Test
    @DisplayName("PRO user should respect 200 referral limit")
    void proUserShouldRespect200ReferralLimit() {
        // Arrange
        User proUser = createVerifiedUserWithPlan(SubscriptionPlan.PRO);

        // Act & Assert
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.REFERRAL, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.REFERRAL, 100)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.REFERRAL, 199)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.REFERRAL, 200)).isFalse();
        assertThat(PermissionEvaluator.canExceedLimit(proUser, ResourceType.REFERRAL, 201)).isFalse();
    }

    @Test
    @DisplayName("ELITE user should have unlimited resources")
    void eliteUserShouldHaveUnlimitedResources() {
        // Arrange
        User eliteUser = createVerifiedUserWithPlan(SubscriptionPlan.ELITE);

        // Act & Assert - Elite has Integer.MAX_VALUE limit
        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.PROPERTY, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.PROPERTY, 1000)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.PROPERTY, 10000)).isTrue();

        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.REFERRAL, 0)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.REFERRAL, 1000)).isTrue();
        assertThat(PermissionEvaluator.canExceedLimit(eliteUser, ResourceType.REFERRAL, 10000)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(SubscriptionPlan.class)
    @DisplayName("All subscription plans should allow basic profile operations")
    void allSubscriptionPlansShouldAllowBasicProfileOperations(SubscriptionPlan plan) {
        // Arrange
        User user = createVerifiedUserWithPlan(plan);

        // Act & Assert
        assertThat(PermissionEvaluator.evaluate(user, Permission.PROFILE_READ)).isTrue();
        assertThat(PermissionEvaluator.evaluate(user, Permission.PROFILE_UPDATE)).isTrue();
    }

    // ========== Helper Methods ==========

    private User createAdminUser() {
        return User.builder()
                .id(USER_ID)
                .email(Email.of("admin@example.com"))
                .name("Admin User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.admin()))
                .activePlan(SubscriptionPlan.ELITE)
                .capabilities(UserCapabilities.defaultCapabilities())
                .accountVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User createVerifiedUserWithPlan(SubscriptionPlan plan) {
        return User.builder()
                .id(USER_ID)
                .email(Email.of(USER_EMAIL))
                .name("User")
                .passwordHash("hashedPassword")
                .roles(Set.of(Role.user()))
                .activePlan(plan)
                .capabilities(UserCapabilities.defaultCapabilities())
                .accountVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}