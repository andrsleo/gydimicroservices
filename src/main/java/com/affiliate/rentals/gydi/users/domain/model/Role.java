package com.affiliate.rentals.gydi.users.domain.model;
import java.util.Objects;

/**
 * Role value object representing a user role in the system.
 *
 * <p>This is an immutable value object following Domain-Driven Design (DDD) principles.
 * It encapsulates a role's identity and name, providing type-safe role management
 * throughout the application. The class includes factory methods for creating
 * common roles and convenience methods for role type checking.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Role guestRole = Role.guest();
 * Role ownerRole = Role.of(1L, RoleName.OWNER);
 * boolean isOwner = ownerRole.isOwner();
 * }</pre>
 *
 * @param id the role's unique identifier, may be {@code null} for transient roles
 * @param name the role name, never {@code null}
 * @author GYDI Development Team
 * @see RoleName
 */
public record Role(Long id, RoleName name) {

    /**
     * Compact constructor that validates the role name.
     *
     * @throws NullPointerException if name is {@code null}
     */
    public Role {
        Objects.requireNonNull(name, "Role name cannot be null");
    }

    /**
     * Factory method to create a Role with a specific ID and name.
     *
     * @param id the role ID
     * @param name the role name
     * @return a new Role instance
     * @throws NullPointerException if name is {@code null}
     */
    public static Role of(Long id, RoleName name) {
        return new Role(id, name);
    }

    /**
     * Factory method to create a USER role without an ID.
     *
     * @return a new USER Role instance
     */
    public static Role user() {
        return new Role(null, RoleName.USER);
    }

    /**
     * Factory method to create an ADMIN role without an ID.
     *
     * @return a new ADMIN Role instance
     */
    public static Role admin() {
        return new Role(null, RoleName.ADMIN);
    }

    /**
     * Checks if this role is a USER role.
     *
     * @return {@code true} if this role is USER, {@code false} otherwise
     */
    public boolean isUser() {
        return RoleName.USER.equals(this.name);
    }

    /**
     * Checks if this role is an ADMIN role.
     *
     * @return {@code true} if this role is ADMIN, {@code false} otherwise
     */
    public boolean isAdmin() {
        return RoleName.ADMIN.equals(this.name);
    }

    /**
     * Checks if this role has system-level access.
     * Currently, only ADMIN has system-level access.
     *
     * @return {@code true} if this role has system access, {@code false} otherwise
     */
    public boolean hasSystemAccess() {
        return isAdmin();
    }
}
