package com.affiliate.rentals.gydi.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link UserCapabilities}.
 *
 * <p>This test class validates the UserCapabilities value object, including:</p>
 * <ul>
 *   <li>Factory method behavior</li>
 *   <li>Immutability guarantees</li>
 *   <li>Capability query methods</li>
 *   <li>Description generation</li>
 *   <li>Record equality and hashCode</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("UserCapabilities Value Object Tests")
class UserCapabilitiesTest {

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("defaultCapabilities should return all capabilities enabled")
    void defaultCapabilitiesShouldReturnAllEnabled() {
        // Act
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities();

        // Assert
        assertThat(capabilities.canPublish()).isTrue();
        assertThat(capabilities.canRefer()).isTrue();
        assertThat(capabilities.canRent()).isTrue();
    }

    @Test
    @DisplayName("allDisabled should return all capabilities disabled")
    void allDisabledShouldReturnAllDisabled() {
        // Act
        UserCapabilities capabilities = UserCapabilities.allDisabled();

        // Assert
        assertThat(capabilities.canPublish()).isFalse();
        assertThat(capabilities.canRefer()).isFalse();
        assertThat(capabilities.canRent()).isFalse();
    }

    @Test
    @DisplayName("of should create capabilities with specific values")
    void ofShouldCreateCapabilitiesWithSpecificValues() {
        // Act
        UserCapabilities capabilities = UserCapabilities.of(true, false, true);

        // Assert
        assertThat(capabilities.canPublish()).isTrue();
        assertThat(capabilities.canRefer()).isFalse();
        assertThat(capabilities.canRent()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, true",
        "true, true, false",
        "true, false, true",
        "true, false, false",
        "false, true, true",
        "false, true, false",
        "false, false, true",
        "false, false, false"
    })
    @DisplayName("of should correctly handle all combinations")
    void ofShouldCorrectlyHandleAllCombinations(boolean publish, boolean refer, boolean rent) {
        // Act
        UserCapabilities capabilities = UserCapabilities.of(publish, refer, rent);

        // Assert
        assertThat(capabilities.canPublish()).isEqualTo(publish);
        assertThat(capabilities.canRefer()).isEqualTo(refer);
        assertThat(capabilities.canRent()).isEqualTo(rent);
    }

    // ========== Immutability Tests ==========

    @Test
    @DisplayName("withPublish should create new instance with modified publish capability")
    void withPublishShouldCreateNewInstanceWithModifiedPublish() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified = original.withPublish(false);

        // Assert
        assertThat(original.canPublish()).isTrue(); // Original unchanged
        assertThat(modified.canPublish()).isFalse(); // New instance modified
        assertThat(modified.canRefer()).isTrue(); // Other capabilities unchanged
        assertThat(modified.canRent()).isTrue();
    }

    @Test
    @DisplayName("withRefer should create new instance with modified refer capability")
    void withReferShouldCreateNewInstanceWithModifiedRefer() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified = original.withRefer(false);

        // Assert
        assertThat(original.canRefer()).isTrue(); // Original unchanged
        assertThat(modified.canRefer()).isFalse(); // New instance modified
        assertThat(modified.canPublish()).isTrue(); // Other capabilities unchanged
        assertThat(modified.canRent()).isTrue();
    }

    @Test
    @DisplayName("withRent should create new instance with modified rent capability")
    void withRentShouldCreateNewInstanceWithModifiedRent() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified = original.withRent(false);

        // Assert
        assertThat(original.canRent()).isTrue(); // Original unchanged
        assertThat(modified.canRent()).isFalse(); // New instance modified
        assertThat(modified.canPublish()).isTrue(); // Other capabilities unchanged
        assertThat(modified.canRefer()).isTrue();
    }

    @Test
    @DisplayName("Chaining with methods should work correctly")
    void chainingWithMethodsShouldWorkCorrectly() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified = original
            .withPublish(false)
            .withRefer(false)
            .withRent(false);

        // Assert
        assertThat(original.canPublish()).isTrue(); // Original unchanged
        assertThat(original.canRefer()).isTrue();
        assertThat(original.canRent()).isTrue();

        assertThat(modified.canPublish()).isFalse(); // Modified correctly
        assertThat(modified.canRefer()).isFalse();
        assertThat(modified.canRent()).isFalse();
    }

    // ========== Query Method Tests ==========

    @Test
    @DisplayName("areAllEnabled should return true when all capabilities are enabled")
    void areAllEnabledShouldReturnTrueWhenAllEnabled() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.of(true, true, true);

        // Act & Assert
        assertThat(capabilities.areAllEnabled()).isTrue();
    }

    @Test
    @DisplayName("areAllEnabled should return false when any capability is disabled")
    void areAllEnabledShouldReturnFalseWhenAnyDisabled() {
        // Act & Assert
        assertThat(UserCapabilities.of(false, true, true).areAllEnabled()).isFalse();
        assertThat(UserCapabilities.of(true, false, true).areAllEnabled()).isFalse();
        assertThat(UserCapabilities.of(true, true, false).areAllEnabled()).isFalse();
        assertThat(UserCapabilities.of(false, false, false).areAllEnabled()).isFalse();
    }

    @Test
    @DisplayName("areAllDisabled should return true when all capabilities are disabled")
    void areAllDisabledShouldReturnTrueWhenAllDisabled() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.of(false, false, false);

        // Act & Assert
        assertThat(capabilities.areAllDisabled()).isTrue();
    }

    @Test
    @DisplayName("areAllDisabled should return false when any capability is enabled")
    void areAllDisabledShouldReturnFalseWhenAnyEnabled() {
        // Act & Assert
        assertThat(UserCapabilities.of(true, false, false).areAllDisabled()).isFalse();
        assertThat(UserCapabilities.of(false, true, false).areAllDisabled()).isFalse();
        assertThat(UserCapabilities.of(false, false, true).areAllDisabled()).isFalse();
        assertThat(UserCapabilities.of(true, true, true).areAllDisabled()).isFalse();
    }

    @Test
    @DisplayName("hasAnyEnabled should return true when at least one capability is enabled")
    void hasAnyEnabledShouldReturnTrueWhenAtLeastOneEnabled() {
        // Act & Assert
        assertThat(UserCapabilities.of(true, false, false).hasAnyEnabled()).isTrue();
        assertThat(UserCapabilities.of(false, true, false).hasAnyEnabled()).isTrue();
        assertThat(UserCapabilities.of(false, false, true).hasAnyEnabled()).isTrue();
        assertThat(UserCapabilities.of(true, true, true).hasAnyEnabled()).isTrue();
    }

    @Test
    @DisplayName("hasAnyEnabled should return false when all capabilities are disabled")
    void hasAnyEnabledShouldReturnFalseWhenAllDisabled() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.of(false, false, false);

        // Act & Assert
        assertThat(capabilities.hasAnyEnabled()).isFalse();
    }

    @Test
    @DisplayName("defaultCapabilities should have all enabled")
    void defaultCapabilitiesShouldHaveAllEnabled() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities();

        // Act & Assert
        assertThat(capabilities.areAllEnabled()).isTrue();
        assertThat(capabilities.areAllDisabled()).isFalse();
        assertThat(capabilities.hasAnyEnabled()).isTrue();
    }

    @Test
    @DisplayName("allDisabled should have all disabled")
    void allDisabledShouldHaveAllDisabledState() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.allDisabled();

        // Act & Assert
        assertThat(capabilities.areAllEnabled()).isFalse();
        assertThat(capabilities.areAllDisabled()).isTrue();
        assertThat(capabilities.hasAnyEnabled()).isFalse();
    }

    // ========== Description Tests ==========

    @Test
    @DisplayName("getEnabledCapabilitiesDescription should return 'All capabilities enabled' when all enabled")
    void getEnabledCapabilitiesDescriptionShouldReturnAllEnabledMessage() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities();

        // Act
        String description = capabilities.getEnabledCapabilitiesDescription();

        // Assert
        assertThat(description).isEqualTo("All capabilities enabled");
    }

    @Test
    @DisplayName("getEnabledCapabilitiesDescription should return 'No capabilities enabled' when all disabled")
    void getEnabledCapabilitiesDescriptionShouldReturnNoCapabilitiesMessage() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.allDisabled();

        // Act
        String description = capabilities.getEnabledCapabilitiesDescription();

        // Assert
        assertThat(description).isEqualTo("No capabilities enabled");
    }

    @Test
    @DisplayName("getEnabledCapabilitiesDescription should list only enabled capabilities")
    void getEnabledCapabilitiesDescriptionShouldListOnlyEnabled() {
        // Test only Publish enabled
        UserCapabilities publishOnly = UserCapabilities.of(true, false, false);
        assertThat(publishOnly.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Publish");

        // Test only Refer enabled
        UserCapabilities referOnly = UserCapabilities.of(false, true, false);
        assertThat(referOnly.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Refer");

        // Test only Rent enabled
        UserCapabilities rentOnly = UserCapabilities.of(false, false, true);
        assertThat(rentOnly.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Rent");

        // Test Publish and Refer enabled
        UserCapabilities publishAndRefer = UserCapabilities.of(true, true, false);
        assertThat(publishAndRefer.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Publish, Refer");

        // Test Publish and Rent enabled
        UserCapabilities publishAndRent = UserCapabilities.of(true, false, true);
        assertThat(publishAndRent.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Publish, Rent");

        // Test Refer and Rent enabled
        UserCapabilities referAndRent = UserCapabilities.of(false, true, true);
        assertThat(referAndRent.getEnabledCapabilitiesDescription()).isEqualTo("Enabled: Refer, Rent");
    }

    // ========== Record Equality Tests ==========

    @Test
    @DisplayName("Capabilities with same values should be equal")
    void capabilitiesWithSameValuesShouldBeEqual() {
        // Arrange
        UserCapabilities cap1 = UserCapabilities.of(true, false, true);
        UserCapabilities cap2 = UserCapabilities.of(true, false, true);

        // Act & Assert
        assertThat(cap1).isEqualTo(cap2);
        assertThat(cap1.hashCode()).isEqualTo(cap2.hashCode());
    }

    @Test
    @DisplayName("Capabilities with different values should not be equal")
    void capabilitiesWithDifferentValuesShouldNotBeEqual() {
        // Arrange
        UserCapabilities cap1 = UserCapabilities.of(true, false, true);
        UserCapabilities cap2 = UserCapabilities.of(true, true, true);

        // Act & Assert
        assertThat(cap1).isNotEqualTo(cap2);
    }

    @Test
    @DisplayName("defaultCapabilities should be equal to of(true, true, true)")
    void defaultCapabilitiesShouldBeEqualToOfAllTrue() {
        // Arrange
        UserCapabilities defaultCaps = UserCapabilities.defaultCapabilities();
        UserCapabilities ofCaps = UserCapabilities.of(true, true, true);

        // Act & Assert
        assertThat(defaultCaps).isEqualTo(ofCaps);
    }

    @Test
    @DisplayName("allDisabled should be equal to of(false, false, false)")
    void allDisabledShouldBeEqualToOfAllFalse() {
        // Arrange
        UserCapabilities allDisabledCaps = UserCapabilities.allDisabled();
        UserCapabilities ofCaps = UserCapabilities.of(false, false, false);

        // Act & Assert
        assertThat(allDisabledCaps).isEqualTo(ofCaps);
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString should include all capability values")
    void toStringShouldIncludeAllCapabilityValues() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.of(true, false, true);

        // Act
        String result = capabilities.toString();

        // Assert
        assertThat(result).contains("UserCapabilities");
        assertThat(result).contains("canPublish=true");
        assertThat(result).contains("canRefer=false");
        assertThat(result).contains("canRent=true");
    }

    // ========== Accessor Tests ==========

    @Test
    @DisplayName("Accessors should return correct values")
    void accessorsShouldReturnCorrectValues() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.of(true, false, true);

        // Act & Assert
        assertThat(capabilities.canPublish()).isTrue();
        assertThat(capabilities.canRefer()).isFalse();
        assertThat(capabilities.canRent()).isTrue();
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Multiple calls to with methods should create independent instances")
    void multipleCallsToWithMethodsShouldCreateIndependentInstances() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified1 = original.withPublish(false);
        UserCapabilities modified2 = original.withPublish(false);

        // Assert
        assertThat(modified1).isEqualTo(modified2); // Equal but not same instance
        assertThat(modified1).isNotSameAs(modified2);
        assertThat(original.canPublish()).isTrue(); // Original unchanged
    }

    @Test
    @DisplayName("Setting capability to same value should create different instance")
    void settingCapabilityToSameValueShouldCreateDifferentInstance() {
        // Arrange
        UserCapabilities original = UserCapabilities.defaultCapabilities();

        // Act
        UserCapabilities modified = original.withPublish(true); // Already true

        // Assert
        assertThat(modified).isEqualTo(original); // Equal values
        assertThat(modified).isNotSameAs(original); // But different instances (immutability)
    }

    // ========== Business Logic Tests ==========

    @Test
    @DisplayName("Disabled publish should prevent publishing")
    void disabledPublishShouldPreventPublishing() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities().withPublish(false);

        // Act & Assert
        assertThat(capabilities.canPublish()).isFalse();
        assertThat(capabilities.canRefer()).isTrue(); // Others still enabled
        assertThat(capabilities.canRent()).isTrue();
    }

    @Test
    @DisplayName("Disabled refer should prevent referrals")
    void disabledReferShouldPreventReferrals() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities().withRefer(false);

        // Act & Assert
        assertThat(capabilities.canRefer()).isFalse();
        assertThat(capabilities.canPublish()).isTrue(); // Others still enabled
        assertThat(capabilities.canRent()).isTrue();
    }

    @Test
    @DisplayName("Disabled rent should prevent renting")
    void disabledRentShouldPreventRenting() {
        // Arrange
        UserCapabilities capabilities = UserCapabilities.defaultCapabilities().withRent(false);

        // Act & Assert
        assertThat(capabilities.canRent()).isFalse();
        assertThat(capabilities.canPublish()).isTrue(); // Others still enabled
        assertThat(capabilities.canRefer()).isTrue();
    }
}