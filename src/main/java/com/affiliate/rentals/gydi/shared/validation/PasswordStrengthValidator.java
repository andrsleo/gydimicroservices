package com.affiliate.rentals.gydi.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for {@link StrongPassword} annotation.
 *
 * <p>
 * This validator enforces strong password requirements to prevent weak
 * passwords
 * that are vulnerable to brute-force attacks, dictionary attacks, and
 * credential stuffing.
 * </p>
 *
 * <p>
 * <b>Validation Rules:</b>
 * </p>
 * <ul>
 * <li>Length: 8-100 characters (configurable)</li>
 * <li>Complexity: Must contain uppercase, lowercase, digit, and special
 * character</li>
 * <li>Blacklist: Rejects common weak passwords (password123, qwerty, etc.)</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Validation happens at the API boundary (before business logic)</li>
 * <li>Failed validation returns 400 Bad Request with detailed error
 * message</li>
 * <li>Passwords are NEVER logged or stored in plain text</li>
 * <li>BCrypt hashing is applied after validation (see UserService)</li>
 * </ul>
 *
 * <p>
 * <b>Performance:</b> Regex compilation is done once per validator instance
 * for optimal performance. Blacklist check is O(1) using HashSet.
 * </p>
 *
 * @see StrongPassword
 * @author GYDI Development Team
 */
@Slf4j
public class PasswordStrengthValidator implements ConstraintValidator<StrongPassword, String> {

    /**
     * Pattern for uppercase letter (A-Z)
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");

    /**
     * Pattern for lowercase letter (a-z)
     */
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");

    /**
     * Pattern for digit (0-9)
     */
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");

    /**
     * Pattern for special character (@$!%*?&#)
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[@$!%*?&#]");

    /**
     * Common passwords blacklist.
     * Based on OWASP Top 10,000 most common passwords and typical patterns.
     *
     * SECURITY: This is a minimal blacklist. For production, consider integrating
     * with Have I Been Pwned API or a comprehensive password blacklist database.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            // Top 10 most common
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",

            // Common patterns
            "password123", "password1", "qwerty123", "welcome",
            "admin", "administrator", "root", "user", "guest",

            // Sequential patterns
            "123456789", "1234567890", "abcdefgh", "qwertyuiop",

            // Keyboard patterns
            "asdfghjkl", "zxcvbnm", "qazwsx", "123qwe",

            // Years
            "password2024", "password2025", "welcome2024",

            // Common with special chars (still weak)
            "password!", "password@", "password#", "password$",
            "qwerty!", "123456!", "admin123!", "welcome!");

    private int minLength;
    private int maxLength;
    private boolean checkCommonPasswords;

    /**
     * Initializes the validator with annotation parameters.
     */
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.checkCommonPasswords = constraintAnnotation.checkCommonPasswords();

        log.debug("PasswordStrengthValidator initialized: minLength={}, maxLength={}, checkCommonPasswords={}",
                minLength, maxLength, checkCommonPasswords);
    }

    /**
     * Validates password strength.
     *
     * @param password the password to validate
     * @param context  constraint validator context (for custom error messages)
     * @return true if password meets all requirements, false otherwise
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or blank passwords are handled by @NotBlank annotation
        if (password == null || password.isBlank()) {
            return true; // Let @NotBlank handle this
        }

        // Disable default error message (we'll add custom ones)
        context.disableDefaultConstraintViolation();

        // 1. Check for exact match against common passwords FIRST
        // (Reject "password", "qwerty", "admin" etc. regardless of length)
        if (checkCommonPasswords && isExactCommonPassword(password)) {
            context.buildConstraintViolationWithTemplate(
                    "This password is too common and easily guessed. Please choose a stronger password")
                    .addConstraintViolation();
            return false;
        }

        // 2. Check length
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                    "Password must be at least " + minLength + " characters long").addConstraintViolation();
            return false;
        }

        if (password.length() > maxLength) {
            context.buildConstraintViolationWithTemplate(
                    "Password must not exceed " + maxLength + " characters").addConstraintViolation();
            return false;
        }

        // 3. Check for common password PREFIX (e.g., "password123456")
        // After length check, before complexity
        if (checkCommonPasswords && hasCommonPasswordPrefix(password)) {
            context.buildConstraintViolationWithTemplate(
                    "This password is too common and easily guessed. Please choose a stronger password")
                    .addConstraintViolation();
            return false;
        }

        // 4. Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter (A-Z)").addConstraintViolation();
            return false;
        }

        // 5. Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter (a-z)").addConstraintViolation();
            return false;
        }

        // 6. Check for digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one number (0-9)").addConstraintViolation();
            return false;
        }

        // 7. Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character (@$!%*?&#)").addConstraintViolation();
            return false;
        }

        // All checks passed
        log.debug("Password validation passed (length: {})", password.length());
        return true;
    }

    /**
     * Checks if password exactly matches a common password (case-insensitive).
     *
     * @param password the password to check
     * @return true if password is an exact match, false otherwise
     */
    private boolean isExactCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();

        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            log.warn("Rejected common password (exact match)");
            return true;
        }

        return false;
    }

    /**
     * Checks if password starts with a common password prefix.
     *
     * @param password the password to check
     * @return true if password has common prefix, false otherwise
     */
    private boolean hasCommonPasswordPrefix(String password) {
        String lowerPassword = password.toLowerCase();

        // Check if password starts with a common password
        // (e.g., "password123456" starts with "password")
        for (String commonPwd : COMMON_PASSWORDS) {
            if (lowerPassword.startsWith(commonPwd) && !lowerPassword.equals(commonPwd)) {
                log.warn("Rejected common password (prefix match: {})", commonPwd);
                return true;
            }
        }

        return false;
    }
}
