package com.affiliate.rentals.gydi.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for strong password requirements.
 *
 * <p>A strong password must meet ALL of the following criteria:</p>
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>Maximum 100 characters</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one special character (@$!%*?&)</li>
 *   <li>Not in the common passwords blacklist</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * public record CreateUserRequest(
 *     @NotBlank @Email String email,
 *     @NotBlank @StrongPassword String password,
 *     String firstName,
 *     String lastName
 * ) {}
 * }</pre>
 *
 * <p><b>Valid Examples:</b></p>
 * <ul>
 *   <li>SecureP@ssw0rd</li>
 *   <li>MyP@ssword123</li>
 *   <li>Test123!@#Valid</li>
 * </ul>
 *
 * <p><b>Invalid Examples:</b></p>
 * <ul>
 *   <li>password (no uppercase, digit, special char)</li>
 *   <li>PASSWORD (no lowercase, digit, special char)</li>
 *   <li>Pass123 (no special char)</li>
 *   <li>Pass@word (no digit)</li>
 *   <li>password123 (common password - blacklisted)</li>
 * </ul>
 *
 * <p><b>Security Note:</b> This validation is applied at the API boundary.
 * Passwords are hashed with BCrypt before storage. Never log or expose
 * passwords in plain text.</p>
 *
 * @see PasswordStrengthValidator
 * @author GYDI Development Team
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Documented
public @interface StrongPassword {

    /**
     * Default validation error message.
     * Can be overridden with custom message.
     */
    String message() default "Password must be at least 8 characters and contain: " +
            "uppercase letter, lowercase letter, number, and special character (@$!%*?&)";

    /**
     * Allows specifying validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Can be used to pass additional information about the error.
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum password length.
     * Default: 8 characters
     */
    int minLength() default 8;

    /**
     * Maximum password length.
     * Default: 100 characters
     */
    int maxLength() default 100;

    /**
     * Whether to check against common passwords blacklist.
     * Default: true
     */
    boolean checkCommonPasswords() default true;
}
