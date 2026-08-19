package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a user in the users schema.
 *
 * <p>
 * This entity maps to the {@code users} table in the {@code users} schema.
 * It stores user information including authentication credentials, personal
 * data,
 * and role associations. The entity maintains a many-to-many relationship with
 * RoleEntity through the {@code user_roles} join table.
 * </p>
 *
 * <p>
 * This is an infrastructure-layer class that implements the persistence
 * details for the User domain model, following hexagonal architecture
 * principles.
 * </p>
 *
 * @author GYDI Development Team
 * @see RoleEntity
 */
@Entity
@Table(name = "users", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /**
     * The unique identifier for this user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user's email address.
     * Must be unique and cannot be null.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * The user's hashed password.
     * Cannot be null.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * The user's phone number.
     * This field is optional.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * The user's active subscription plan.
     * Determines commission rates and resource limits.
     * Defaults to FREE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "active_plan", nullable = false, length = 20)
    private SubscriptionPlanEntity activePlan = SubscriptionPlanEntity.FREE;

    /**
     * Flag indicating if the user can publish properties.
     * Defaults to true.
     */
    @Column(name = "can_publish", nullable = false)
    private Boolean canPublish = true;

    /**
     * Flag indicating if the user can generate referral links.
     * Defaults to true.
     */
    @Column(name = "can_refer", nullable = false)
    private Boolean canRefer = true;

    /**
     * Flag indicating if the user can rent/book properties.
     * Defaults to true.
     */
    @Column(name = "can_rent", nullable = false)
    private Boolean canRent = true;

    /**
     * Flag indicating if the user's account is verified.
     * Some features require verification (e.g., publishing, referrals).
     * Defaults to false.
     */
    @Column(name = "account_verified", nullable = false)
    private Boolean accountVerified = false;

    /**
     * The roles assigned to this user.
     * This is the owning side of the many-to-many relationship.
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "user_roles", schema = "users", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    /**
     * The timestamp when this user was created.
     * Automatically set on persist.
     */
    @Column(name = "is_verified_creator", nullable = false)
    private Boolean isVerifiedCreator = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Adds a role to this user's role set.
     *
     * @param role the role to add
     */
    public void addRole(RoleEntity role) {
        roles.add(role);
        role.getUsers().add(this);
    }

    /**
     * Removes a role from this user's role set.
     *
     * @param role the role to remove
     */
    public void removeRole(RoleEntity role) {
        roles.remove(role);
        role.getUsers().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "UserEntity{id=%d, email='%s', roles=%d}"
                .formatted(id, email, roles.size());
    }
}
