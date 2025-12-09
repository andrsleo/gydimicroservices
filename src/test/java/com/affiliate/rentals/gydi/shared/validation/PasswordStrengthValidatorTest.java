package com.affiliate.rentals.gydi.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PasswordStrengthValidator}.
 *
 * <p>
 * Tests cover:
 * </p>
 * <ul>
 * <li>Valid strong passwords</li>
 * <li>Invalid passwords (too short, missing complexity, common passwords)</li>
 * <li>Edge cases (null, blank, max length)</li>
 * <li>Custom error messages</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@DisplayName("Password Strength Validator Tests")
class PasswordStrengthValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Test DTO for validation testing
     */
    record TestPasswordDto(@StrongPassword String password) {
    }

    // ========================================================================
    // Valid Passwords (Should Pass)
    // ========================================================================

    @ParameterizedTest(name = "Valid password: {0}")
    @ValueSource(strings = {
            "SecureP@ss123", // Standard strong password
            "MyP@ssword2024", // With year
            "Test123!@#Valid", // Multiple special chars
            "Ab1@cdef", // Minimum valid
            "VeryL0ng&SecureP@sswordWith100CharacterLimit1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()1234", // Long
                                                                                                              // password
            "Complex!Pass123word", // Complex
            "Str0ng!P@ss", // All requirements met
            "MySecure#Pass2025", // Future year
            "T3st!ngP@ss" // Testing password
    // Note: "Admin!123Secure" removed - starts with "admin" which is a common
    // password
    })
    @DisplayName("Should accept valid strong passwords")
    void shouldAcceptValidStrongPasswords(String password) {
        TestPasswordDto dto = new TestPasswordDto(password);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    // ========================================================================
    // Invalid Passwords - Too Short
    // ========================================================================

    @ParameterizedTest(name = "Too short: {0}")
    @ValueSource(strings = {
            "Ab1@", // 4 chars
            "Test1!", // 6 chars
            "Pass1!", // 6 chars
            "Abc123!" // 7 chars (< 8)
    })
    @DisplayName("Should reject passwords that are too short")
    void shouldRejectTooShortPasswords(String password) {
        TestPasswordDto dto = new TestPasswordDto(password);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("at least 8 characters");
    }

    // ========================================================================
    // Invalid Passwords - Missing Complexity
    // ========================================================================

    @Test
    @DisplayName("Should reject password without uppercase")
    void shouldRejectPasswordWithoutUppercase() {
        TestPasswordDto dto = new TestPasswordDto("lowercase123!");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("uppercase letter");
    }

    @Test
    @DisplayName("Should reject password without lowercase")
    void shouldRejectPasswordWithoutLowercase() {
        TestPasswordDto dto = new TestPasswordDto("UPPERCASE123!");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("lowercase letter");
    }

    @Test
    @DisplayName("Should reject password without digit")
    void shouldRejectPasswordWithoutDigit() {
        TestPasswordDto dto = new TestPasswordDto("NoDigitsHere!");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("number");
    }

    @Test
    @DisplayName("Should reject password without special character")
    void shouldRejectPasswordWithoutSpecialChar() {
        TestPasswordDto dto = new TestPasswordDto("NoSpecialChar123");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("special character");
    }

    // ========================================================================
    // Invalid Passwords - Common Passwords
    // ========================================================================

    @ParameterizedTest(name = "Common password: {0}")
    @ValueSource(strings = {
            "password", // Most common
            "123456", // Very common
            "qwerty", // Keyboard pattern
            "password123", // Common with numbers
            "Password123!", // Common with complexity (still blacklisted)
            "admin", // Common admin password
            "welcome", // Common welcome password
            "letmein", // Common phrase
            "trustno1", // Common movie reference
            "password!", // Common with special char
            "qwerty123", // Keyboard + numbers
            "password2024", // With year
            "administrator", // Common role
            "password@", // With special but still common
            "123456!" // Sequential with special
    })
    @DisplayName("Should reject common weak passwords")
    void shouldRejectCommonPasswords(String password) {
        TestPasswordDto dto = new TestPasswordDto(password);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("too common");
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle null password (delegated to @NotBlank)")
    void shouldHandleNullPassword() {
        TestPasswordDto dto = new TestPasswordDto(null);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        // @StrongPassword allows null (delegates to @NotBlank)
        // In real DTO, @NotBlank would catch this
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle blank password (delegated to @NotBlank)")
    void shouldHandleBlankPassword() {
        TestPasswordDto dto = new TestPasswordDto("   ");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        // @StrongPassword allows blank (delegates to @NotBlank)
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject password exceeding max length")
    void shouldRejectPasswordExceedingMaxLength() {
        // 101 characters (max is 100)
        String tooLong = "A".repeat(50) + "a".repeat(45) + "1@" + "x".repeat(4);
        TestPasswordDto dto = new TestPasswordDto(tooLong);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("100 characters");
    }

    @Test
    @DisplayName("Should accept password at max length")
    void shouldAcceptPasswordAtMaxLength() {
        // Exactly 100 characters with all requirements
        String maxLength = "A".repeat(40) + "a".repeat(40) + "1234567890" + "@!$%&*?@!@";
        TestPasswordDto dto = new TestPasswordDto(maxLength);
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    // ========================================================================
    // Specific Security Scenarios
    // ========================================================================

    @Test
    @DisplayName("Should reject password with prefix matching common password")
    void shouldRejectPasswordWithCommonPrefix() {
        // "password" is common, so "password123456789" should also be rejected
        TestPasswordDto dto = new TestPasswordDto("password123456789");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("too common");
    }

    @Test
    @DisplayName("Should accept password similar to common but with modifications")
    void shouldAcceptPasswordSimilarToCommonButModified() {
        // "passwerd" (typo) is not in blacklist, and meets complexity
        TestPasswordDto dto = new TestPasswordDto("Passwerd123!");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should accept password with all allowed special characters")
    void shouldAcceptPasswordWithAllSpecialChars() {
        TestPasswordDto dto = new TestPasswordDto("Aa1@$!%*?&");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    // ========================================================================
    // Real-World Scenarios
    // ========================================================================

    @Test
    @DisplayName("Should reject user trying to use 'Password123!' (common pattern)")
    void shouldRejectCommonPatternPassword() {
        TestPasswordDto dto = new TestPasswordDto("Password123!");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("too common");
    }

    @Test
    @DisplayName("Should accept legitimate strong password from password manager")
    void shouldAcceptPasswordManagerGeneratedPassword() {
        TestPasswordDto dto = new TestPasswordDto("Xk9$mL2@pQ7&");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should accept passphrase-style password")
    void shouldAcceptPassphraseStylePassword() {
        TestPasswordDto dto = new TestPasswordDto("MyDog!sNamed7Buddy");
        Set<ConstraintViolation<TestPasswordDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
