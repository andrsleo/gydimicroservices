package com.affiliate.rentals.gydi.users.application.dto;

/**
 * Response for permission check operations.
 *
 * <p>This record encapsulates the result of a permission evaluation,
 * indicating whether the user has the requested permission and providing
 * additional context about the decision.</p>
 *
 * @param userId the unique identifier of the user
 * @param permission the permission that was checked
 * @param granted whether the permission was granted
 * @param reason optional reason for the decision (e.g., "Account not verified", "Admin override")
 * @author GYDI Development Team
 */
public record PermissionCheckResponse(
        Long userId,
        String permission,
        boolean granted,
        String reason
) {
    /**
     * Creates a granted permission response.
     *
     * @param userId the user ID
     * @param permission the permission name
     * @return a granted response
     */
    public static PermissionCheckResponse granted(Long userId, String permission) {
        return new PermissionCheckResponse(userId, permission, true, "Permission granted");
    }

    /**
     * Creates a granted permission response with a custom reason.
     *
     * @param userId the user ID
     * @param permission the permission name
     * @param reason the reason for granting
     * @return a granted response
     */
    public static PermissionCheckResponse granted(Long userId, String permission, String reason) {
        return new PermissionCheckResponse(userId, permission, true, reason);
    }

    /**
     * Creates a denied permission response.
     *
     * @param userId the user ID
     * @param permission the permission name
     * @param reason the reason for denial
     * @return a denied response
     */
    public static PermissionCheckResponse denied(Long userId, String permission, String reason) {
        return new PermissionCheckResponse(userId, permission, false, reason);
    }
}