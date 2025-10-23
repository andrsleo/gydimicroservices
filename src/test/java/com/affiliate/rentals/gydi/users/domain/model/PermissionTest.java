package com.affiliate.rentals.gydi.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link Permission}.
 *
 * <p>This test class validates the Permission enum helper methods, including:</p>
 * <ul>
 *   <li>Admin-only permission identification</li>
 *   <li>Verification requirement checks</li>
 *   <li>Capability requirement checks</li>
 *   <li>Resource limit applicability</li>
 *   <li>Paid plan requirement checks</li>
 *   <li>Permission descriptions</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("Permission Enum Tests")
class PermissionTest {

    // ========== Admin-Only Permission Tests ==========

    @Test
    @DisplayName("All admin permissions should be identified as admin-only")
    void allAdminPermissionsShouldBeIdentifiedAsAdminOnly() {
        // Define expected admin-only permissions
        Set<Permission> adminPermissions = EnumSet.of(
            Permission.PROPERTY_UPDATE_ANY,
            Permission.PROPERTY_DELETE_ANY,
            Permission.PROPERTY_VIEW_ANY,
            Permission.PROPERTY_MODERATE,
            Permission.REFERRAL_VIEW_ANY,
            Permission.BOOKING_VIEW_ANY,
            Permission.ANALYTICS_VIEW_GLOBAL,
            Permission.USER_MANAGE,
            Permission.SUBSCRIPTION_MANAGE,
            Permission.SYSTEM_CONFIGURE,
            Permission.MODERATION_MANAGE,
            Permission.PAYMENT_VIEW,
            Permission.REPORT_GENERATE
        );

        // Act & Assert
        for (Permission permission : adminPermissions) {
            assertThat(permission.isAdminOnly())
                .as("Permission %s should be admin-only", permission)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Regular user permissions should NOT be admin-only")
    void regularUserPermissionsShouldNotBeAdminOnly() {
        // Define regular user permissions
        Set<Permission> userPermissions = EnumSet.of(
            Permission.PROFILE_READ,
            Permission.PROFILE_UPDATE,
            Permission.PROFILE_DELETE,
            Permission.PROPERTY_PUBLISH,
            Permission.PROPERTY_UPDATE_OWN,
            Permission.PROPERTY_DELETE_OWN,
            Permission.REFERRAL_GENERATE,
            Permission.REFERRAL_VIEW_OWN,
            Permission.PROPERTY_RENT,
            Permission.BOOKING_CREATE,
            Permission.BOOKING_VIEW_OWN,
            Permission.ANALYTICS_VIEW_OWN,
            Permission.ANALYTICS_VIEW_ADVANCED,
            Permission.ANALYTICS_EXPORT
        );

        // Act & Assert
        for (Permission permission : userPermissions) {
            assertThat(permission.isAdminOnly())
                .as("Permission %s should NOT be admin-only", permission)
                .isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {
        "PROPERTY_UPDATE_ANY", "PROPERTY_DELETE_ANY", "PROPERTY_VIEW_ANY",
        "PROPERTY_MODERATE", "REFERRAL_VIEW_ANY", "BOOKING_VIEW_ANY",
        "ANALYTICS_VIEW_GLOBAL", "USER_MANAGE", "SUBSCRIPTION_MANAGE",
        "SYSTEM_CONFIGURE", "MODERATION_MANAGE", "PAYMENT_VIEW", "REPORT_GENERATE"
    })
    @DisplayName("Specific admin permissions should be admin-only")
    void specificAdminPermissionsShouldBeAdminOnly(Permission permission) {
        // Act & Assert
        assertThat(permission.isAdminOnly()).isTrue();
    }

    // ========== Verification Requirement Tests ==========

    @Test
    @DisplayName("PROPERTY_PUBLISH should require verification")
    void propertyPublishShouldRequireVerification() {
        // Act & Assert
        assertThat(Permission.PROPERTY_PUBLISH.requiresVerification()).isTrue();
    }

    @Test
    @DisplayName("REFERRAL_GENERATE should require verification")
    void referralGenerateShouldRequireVerification() {
        // Act & Assert
        assertThat(Permission.REFERRAL_GENERATE.requiresVerification()).isTrue();
    }

    @Test
    @DisplayName("Only PROPERTY_PUBLISH and REFERRAL_GENERATE should require verification")
    void onlyPropertyPublishAndReferralGenerateShouldRequireVerification() {
        // Arrange
        Set<Permission> verificationRequired = EnumSet.of(
            Permission.PROPERTY_PUBLISH,
            Permission.REFERRAL_GENERATE
        );

        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (verificationRequired.contains(permission)) {
                assertThat(permission.requiresVerification())
                    .as("Permission %s should require verification", permission)
                    .isTrue();
            } else {
                assertThat(permission.requiresVerification())
                    .as("Permission %s should NOT require verification", permission)
                    .isFalse();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {
        "PROFILE_READ", "PROFILE_UPDATE", "ANALYTICS_VIEW_OWN", "BOOKING_VIEW_OWN"
    })
    @DisplayName("Basic permissions should NOT require verification")
    void basicPermissionsShouldNotRequireVerification(Permission permission) {
        // Act & Assert
        assertThat(permission.requiresVerification()).isFalse();
    }

    // ========== Capability Check Tests ==========

    @Test
    @DisplayName("Permissions requiring capability checks should be identified correctly")
    void permissionsRequiringCapabilityChecksShouldBeIdentifiedCorrectly() {
        // Arrange
        Set<Permission> capabilityRequired = EnumSet.of(
            Permission.PROPERTY_PUBLISH,
            Permission.REFERRAL_GENERATE,
            Permission.PROPERTY_RENT,
            Permission.BOOKING_CREATE
        );

        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (capabilityRequired.contains(permission)) {
                assertThat(permission.requiresCapabilityCheck())
                    .as("Permission %s should require capability check", permission)
                    .isTrue();
            } else {
                assertThat(permission.requiresCapabilityCheck())
                    .as("Permission %s should NOT require capability check", permission)
                    .isFalse();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {
        "PROPERTY_PUBLISH", "REFERRAL_GENERATE", "PROPERTY_RENT", "BOOKING_CREATE"
    })
    @DisplayName("Specific permissions should require capability check")
    void specificPermissionsShouldRequireCapabilityCheck(Permission permission) {
        // Act & Assert
        assertThat(permission.requiresCapabilityCheck()).isTrue();
    }

    // ========== Resource Limit Tests ==========

    @Test
    @DisplayName("PROPERTY_PUBLISH should have resource limits")
    void propertyPublishShouldHaveResourceLimits() {
        // Act & Assert
        assertThat(Permission.PROPERTY_PUBLISH.hasResourceLimits()).isTrue();
    }

    @Test
    @DisplayName("REFERRAL_GENERATE should have resource limits")
    void referralGenerateShouldHaveResourceLimits() {
        // Act & Assert
        assertThat(Permission.REFERRAL_GENERATE.hasResourceLimits()).isTrue();
    }

    @Test
    @DisplayName("Only PROPERTY_PUBLISH and REFERRAL_GENERATE should have resource limits")
    void onlyPropertyPublishAndReferralGenerateShouldHaveResourceLimits() {
        // Arrange
        Set<Permission> limitedPermissions = EnumSet.of(
            Permission.PROPERTY_PUBLISH,
            Permission.REFERRAL_GENERATE
        );

        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (limitedPermissions.contains(permission)) {
                assertThat(permission.hasResourceLimits())
                    .as("Permission %s should have resource limits", permission)
                    .isTrue();
            } else {
                assertThat(permission.hasResourceLimits())
                    .as("Permission %s should NOT have resource limits", permission)
                    .isFalse();
            }
        }
    }

    // ========== Paid Plan Requirement Tests ==========

    @Test
    @DisplayName("ANALYTICS_VIEW_ADVANCED should require paid plan")
    void analyticsViewAdvancedShouldRequirePaidPlan() {
        // Act & Assert
        assertThat(Permission.ANALYTICS_VIEW_ADVANCED.requiresPaidPlan()).isTrue();
    }

    @Test
    @DisplayName("ANALYTICS_EXPORT should require paid plan")
    void analyticsExportShouldRequirePaidPlan() {
        // Act & Assert
        assertThat(Permission.ANALYTICS_EXPORT.requiresPaidPlan()).isTrue();
    }

    @Test
    @DisplayName("Only analytics permissions should require paid plan")
    void onlyAnalyticsPermissionsShouldRequirePaidPlan() {
        // Arrange
        Set<Permission> paidPlanRequired = EnumSet.of(
            Permission.ANALYTICS_VIEW_ADVANCED,
            Permission.ANALYTICS_EXPORT
        );

        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (paidPlanRequired.contains(permission)) {
                assertThat(permission.requiresPaidPlan())
                    .as("Permission %s should require paid plan", permission)
                    .isTrue();
            } else {
                assertThat(permission.requiresPaidPlan())
                    .as("Permission %s should NOT require paid plan", permission)
                    .isFalse();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {
        "PROFILE_READ", "PROPERTY_PUBLISH", "REFERRAL_GENERATE"
    })
    @DisplayName("Basic permissions should NOT require paid plan")
    void basicPermissionsShouldNotRequirePaidPlan(Permission permission) {
        // Act & Assert
        assertThat(permission.requiresPaidPlan()).isFalse();
    }

    // ========== Description Tests ==========

    @ParameterizedTest
    @EnumSource(Permission.class)
    @DisplayName("All permissions should have non-empty descriptions")
    void allPermissionsShouldHaveNonEmptyDescriptions(Permission permission) {
        // Act
        String description = permission.getDescription();

        // Assert
        assertThat(description)
            .as("Permission %s should have a description", permission)
            .isNotNull()
            .isNotEmpty()
            .isNotBlank();
    }

    @Test
    @DisplayName("Profile permissions should have correct descriptions")
    void profilePermissionsShouldHaveCorrectDescriptions() {
        // Act & Assert
        assertThat(Permission.PROFILE_READ.getDescription()).isEqualTo("Read own profile");
        assertThat(Permission.PROFILE_UPDATE.getDescription()).isEqualTo("Update own profile");
        assertThat(Permission.PROFILE_DELETE.getDescription()).isEqualTo("Delete own profile");
    }

    @Test
    @DisplayName("Property permissions should have correct descriptions")
    void propertyPermissionsShouldHaveCorrectDescriptions() {
        // Act & Assert
        assertThat(Permission.PROPERTY_PUBLISH.getDescription()).isEqualTo("Publish properties");
        assertThat(Permission.PROPERTY_UPDATE_OWN.getDescription()).isEqualTo("Update own properties");
        assertThat(Permission.PROPERTY_DELETE_OWN.getDescription()).isEqualTo("Delete own properties");
        assertThat(Permission.PROPERTY_VIEW_ANY.getDescription()).isEqualTo("View any property");
        assertThat(Permission.PROPERTY_UPDATE_ANY.getDescription()).isEqualTo("Update any property");
        assertThat(Permission.PROPERTY_DELETE_ANY.getDescription()).isEqualTo("Delete any property");
        assertThat(Permission.PROPERTY_MODERATE.getDescription()).isEqualTo("Moderate properties");
    }

    @Test
    @DisplayName("Referral permissions should have correct descriptions")
    void referralPermissionsShouldHaveCorrectDescriptions() {
        // Act & Assert
        assertThat(Permission.REFERRAL_GENERATE.getDescription()).isEqualTo("Generate referral links");
        assertThat(Permission.REFERRAL_VIEW_OWN.getDescription()).isEqualTo("View own referrals");
        assertThat(Permission.REFERRAL_VIEW_ANY.getDescription()).isEqualTo("View any referrals");
    }

    @Test
    @DisplayName("Analytics permissions should have correct descriptions")
    void analyticsPermissionsShouldHaveCorrectDescriptions() {
        // Act & Assert
        assertThat(Permission.ANALYTICS_VIEW_OWN.getDescription()).isEqualTo("View own analytics");
        assertThat(Permission.ANALYTICS_VIEW_ADVANCED.getDescription()).isEqualTo("View advanced analytics");
        assertThat(Permission.ANALYTICS_EXPORT.getDescription()).isEqualTo("Export analytics");
        assertThat(Permission.ANALYTICS_VIEW_GLOBAL.getDescription()).isEqualTo("View global analytics");
    }

    @Test
    @DisplayName("Admin permissions should have correct descriptions")
    void adminPermissionsShouldHaveCorrectDescriptions() {
        // Act & Assert
        assertThat(Permission.USER_MANAGE.getDescription()).isEqualTo("Manage users");
        assertThat(Permission.SUBSCRIPTION_MANAGE.getDescription()).isEqualTo("Manage subscriptions");
        assertThat(Permission.SYSTEM_CONFIGURE.getDescription()).isEqualTo("Configure system");
        assertThat(Permission.MODERATION_MANAGE.getDescription()).isEqualTo("Manage moderation");
        assertThat(Permission.PAYMENT_VIEW.getDescription()).isEqualTo("View payments");
        assertThat(Permission.REPORT_GENERATE.getDescription()).isEqualTo("Generate reports");
    }

    // ========== Enum Behavior Tests ==========

    @Test
    @DisplayName("Permission enum should have correct number of values")
    void permissionEnumShouldHaveCorrectNumberOfValues() {
        // Act
        Permission[] permissions = Permission.values();

        // Assert
        assertThat(permissions).hasSize(27); // Total permissions defined
    }

    @Test
    @DisplayName("Permission valueOf should work correctly")
    void permissionValueOfShouldWorkCorrectly() {
        // Act & Assert
        assertThat(Permission.valueOf("PROFILE_READ")).isEqualTo(Permission.PROFILE_READ);
        assertThat(Permission.valueOf("PROPERTY_PUBLISH")).isEqualTo(Permission.PROPERTY_PUBLISH);
        assertThat(Permission.valueOf("USER_MANAGE")).isEqualTo(Permission.USER_MANAGE);
    }

    // ========== Cross-Cutting Tests ==========

    @Test
    @DisplayName("Admin-only permissions should NOT require verification")
    void adminOnlyPermissionsShouldNotRequireVerification() {
        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (permission.isAdminOnly()) {
                assertThat(permission.requiresVerification())
                    .as("Admin-only permission %s should not require verification", permission)
                    .isFalse();
            }
        }
    }

    @Test
    @DisplayName("Admin-only permissions should NOT have resource limits")
    void adminOnlyPermissionsShouldNotHaveResourceLimits() {
        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (permission.isAdminOnly()) {
                assertThat(permission.hasResourceLimits())
                    .as("Admin-only permission %s should not have resource limits", permission)
                    .isFalse();
            }
        }
    }

    @Test
    @DisplayName("Permissions with resource limits should require capability check")
    void permissionsWithResourceLimitsShouldRequireCapabilityCheck() {
        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (permission.hasResourceLimits()) {
                assertThat(permission.requiresCapabilityCheck())
                    .as("Permission %s with resource limits should require capability check", permission)
                    .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Permissions requiring verification should also require capability check")
    void permissionsRequiringVerificationShouldAlsoRequireCapabilityCheck() {
        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (permission.requiresVerification()) {
                assertThat(permission.requiresCapabilityCheck())
                    .as("Permission %s requiring verification should also require capability check", permission)
                    .isTrue();
            }
        }
    }

    // ========== Permission Category Tests ==========

    @Test
    @DisplayName("Profile permissions should be correctly categorized")
    void profilePermissionsShouldBeCorrectlyCategorized() {
        // Arrange
        Set<Permission> profilePermissions = EnumSet.of(
            Permission.PROFILE_READ,
            Permission.PROFILE_UPDATE,
            Permission.PROFILE_DELETE
        );

        // Act & Assert
        for (Permission permission : profilePermissions) {
            assertThat(permission.name()).startsWith("PROFILE_");
            assertThat(permission.isAdminOnly()).isFalse();
            assertThat(permission.requiresVerification()).isFalse();
        }
    }

    @Test
    @DisplayName("Property permissions should include both user and admin permissions")
    void propertyPermissionsShouldIncludeBothUserAndAdminPermissions() {
        // Arrange
        Set<Permission> userPropertyPermissions = EnumSet.of(
            Permission.PROPERTY_PUBLISH,
            Permission.PROPERTY_UPDATE_OWN,
            Permission.PROPERTY_DELETE_OWN
        );

        Set<Permission> adminPropertyPermissions = EnumSet.of(
            Permission.PROPERTY_VIEW_ANY,
            Permission.PROPERTY_UPDATE_ANY,
            Permission.PROPERTY_DELETE_ANY,
            Permission.PROPERTY_MODERATE
        );

        // Act & Assert - User permissions
        for (Permission permission : userPropertyPermissions) {
            assertThat(permission.name()).startsWith("PROPERTY_");
            assertThat(permission.isAdminOnly()).isFalse();
        }

        // Act & Assert - Admin permissions
        for (Permission permission : adminPropertyPermissions) {
            assertThat(permission.name()).startsWith("PROPERTY_");
            assertThat(permission.isAdminOnly()).isTrue();
        }
    }

    @Test
    @DisplayName("All permissions requiring paid plan should be analytics-related")
    void allPermissionsRequiringPaidPlanShouldBeAnalyticsRelated() {
        // Act & Assert
        for (Permission permission : Permission.values()) {
            if (permission.requiresPaidPlan()) {
                assertThat(permission.name())
                    .as("Permission %s requiring paid plan should be analytics-related", permission)
                    .startsWith("ANALYTICS_");
            }
        }
    }

    @Test
    @DisplayName("Booking permissions should have correct categorization")
    void bookingPermissionsShouldHaveCorrectCategorization() {
        // Act & Assert
        assertThat(Permission.BOOKING_CREATE.isAdminOnly()).isFalse();
        assertThat(Permission.BOOKING_CREATE.requiresCapabilityCheck()).isTrue();

        assertThat(Permission.BOOKING_VIEW_OWN.isAdminOnly()).isFalse();
        assertThat(Permission.BOOKING_VIEW_OWN.requiresCapabilityCheck()).isFalse();

        assertThat(Permission.BOOKING_VIEW_ANY.isAdminOnly()).isTrue();
    }
}