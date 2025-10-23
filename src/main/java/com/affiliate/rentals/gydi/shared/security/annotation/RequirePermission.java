package com.affiliate.rentals.gydi.shared.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.affiliate.rentals.gydi.users.domain.model.Permission;

/**
 * Annotation to enforce permission checks on methods.
 *
 * <p>When applied to a method, this annotation triggers an AOP aspect that validates
 * whether the currently authenticated user has the required permission before executing
 * the method.</p>
 *
 * <p>The permission check uses the {@code PermissionEvaluator} domain service, which
 * considers:</p>
 * <ul>
 *   <li>User role (ADMIN has all permissions)</li>
 *   <li>Account verification status</li>
 *   <li>User capabilities (canPublish, canRefer, canRent)</li>
 *   <li>Subscription plan level</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * @RequirePermission(Permission.PROPERTY_PUBLISH)
 * public PropertyResponse createProperty(CreatePropertyRequest request) {
 *     // Method implementation
 * }
 * }</pre>
 *
 * <p><strong>Behavior:</strong></p>
 * <ul>
 *   <li>If the user has the permission, the method executes normally</li>
 *   <li>If the user lacks the permission, a {@code PermissionDeniedException} is thrown</li>
 *   <li>If no user is authenticated, a {@code PermissionDeniedException} is thrown</li>
 * </ul>
 *
 * @author GYDI Development Team
 * @see Permission
 * @see com.affiliate.rentals.gydi.users.domain.service.PermissionEvaluator
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * The permission required to execute the annotated method.
     *
     * @return the required permission
     */
    Permission value();

    /**
     * Optional custom error message when permission is denied.
     *
     * <p>If not specified, a default message will be generated.</p>
     *
     * @return the custom error message, or empty string for default
     */
    String message() default "";
}