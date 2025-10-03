package com.affiliate.rentals.gydi.users.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JPA entity representing a role in the users schema.
 *
 * <p>This entity maps to the {@code roles} table in the {@code users} schema.
 * It defines user roles such as ADMIN, OWNER, and GUEST. The entity maintains
 * a many-to-many relationship with UserEntity through the {@code user_roles}
 * join table.</p>
 *
 * <p>This is an infrastructure-layer class that implements the persistence
 * details for the Role domain model, following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 * @see UserEntity
 */
@Entity
@Table(name = "roles", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    /**
     * The unique identifier for this role.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the role (e.g., ADMIN, OWNER, GUEST).
     * This field is unique and cannot be null.
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * The users that have this role.
     * This is the inverse side of the many-to-many relationship.
     */
    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();

    /**
     * Constructs a RoleEntity with the specified name.
     *
     * @param name the role name
     */
    public RoleEntity(String name) {
        this.name = name;
    }

    /**
     * Constructs a RoleEntity with the specified id and name.
     *
     * @param id the role ID
     * @param name the role name
     */
    public RoleEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleEntity that = (RoleEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "RoleEntity{id=%d, name='%s'}".formatted(id, name);
    }
}
