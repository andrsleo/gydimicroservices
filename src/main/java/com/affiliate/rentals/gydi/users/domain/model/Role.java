package com.affiliate.rentals.gydi.users.domain.model;
import java.util.Objects;

/**
 * Role value object - represents a user role.
 * Immutable by design following DDD principles.
 */
public record Role(Long id, RoleName name) {

    public Role {
        Objects.requireNonNull(name, "Role name cannot be null");
    }

    public static Role of(Long id, RoleName name) {
        return new Role(id, name);
    }

    public static Role owner() {
        return new Role(null, RoleName.OWNER);
    }

    public static Role guest() {
        return new Role(null, RoleName.GUEST);
    }

    public static Role admin() {
        return new Role(null, RoleName.ADMIN);
    }

    public boolean isOwner() {
        return RoleName.OWNER.equals(this.name);
    }

    public boolean isGuest() {
        return RoleName.GUEST.equals(this.name);
    }

    public boolean isAdmin() {
        return RoleName.ADMIN.equals(this.name);
    }
}
