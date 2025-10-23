package com.affiliate.rentals.gydi.users.domain.exception;

import org.springframework.http.HttpStatus;

import com.affiliate.rentals.gydi.shared.exception.HttpStatusMapping;
import com.affiliate.rentals.gydi.users.domain.model.Permission;

/**
 * Exception thrown when a user attempts to perform an action they don't have permission for.
 *
 * <p>This exception is part of the domain exception hierarchy and represents a business rule
 * violation where a user lacks the required permission to execute a specific operation.</p>
 *
 * <p>The exception includes details about the denied permission and the reason for denial,
 * which can be used for audit logging and providing meaningful feedback to users.</p>
 *
 * @author GYDI Development Team
 */
@HttpStatusMapping(status = HttpStatus.FORBIDDEN, errorType = "Permission Denied")
public final class PermissionDeniedException extends DomainException {

    private final Long userId;
    private final Permission permission;
    private final String reason;

    /**
     * Private constructor for creating permission denied exceptions.
     *
     * @param userId the ID of the user who was denied access
     * @param permission the permission that was required
     * @param reason the reason for denial
     */
    private PermissionDeniedException(Long userId, Permission permission, String reason) {
        super("User %d does not have permission %s: %s".formatted(userId, permission, reason));
        this.userId = userId;
        this.permission = permission;
        this.reason = reason;
    }

    /**
     * Creates a permission denied exception for a specific user and permission.
     *
     * @param userId the ID of the user who was denied access
     * @param permission the permission that was required
     * @param reason the reason for denial
     * @return a new PermissionDeniedException
     */
    public static PermissionDeniedException forUser(Long userId, Permission permission, String reason) {
        return new PermissionDeniedException(userId, permission, reason);
    }

    /**
     * Creates a permission denied exception with a generic message.
     *
     * @param permission the permission that was required
     * @return a new PermissionDeniedException
     */
    public static PermissionDeniedException forPermission(Permission permission) {
        return new PermissionDeniedException(null, permission, "Permission denied");
    }

    /**
     * Gets the user ID associated with this exception.
     *
     * @return the user ID, or null if not set
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Gets the permission that was required.
     *
     * @return the required permission
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Gets the reason for denial.
     *
     * @return the reason string
     */
    public String getReason() {
        return reason;
    }
}