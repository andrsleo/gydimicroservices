package com.affiliate.rentals.gydi.users.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * User domain entity - represents a user in the system.
 * Immutable by design following clean code principles.
 */
public final class User {
    private final Long id;
    private final String name;
    private final Email email;
    private final String passwordHash;
    private final Set<Role> roles;
    private final LocalDateTime createdAt;

    private User(Builder builder) {
        this.id = builder.id;
        this.name = Objects.requireNonNull(builder.name, "Name cannot be null");
        this.email = Objects.requireNonNull(builder.email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "Password hash cannot be null");
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<Role> roles() {
        return roles;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream()
                .anyMatch(role -> role.name().equals(roleName));
    }

    public User addRole(Role role) {
        var updatedRoles = new HashSet<>(this.roles);
        updatedRoles.add(role);
        return User.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .passwordHash(this.passwordHash)
                .roles(updatedRoles)
                .createdAt(this.createdAt)
                .build();
    }

    public User removeRole(Role role) {
        var updatedRoles = new HashSet<>(this.roles);
        updatedRoles.remove(role);
        return User.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .passwordHash(this.passwordHash)
                .roles(updatedRoles)
                .createdAt(this.createdAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User[id=%d, name=%s, email=%s, roles=%s]"
                .formatted(id, name, email, roles);
    }

    public static final class Builder {
        private Long id;
        private String name;
        private Email email;
        private String passwordHash;
        private Set<Role> roles = new HashSet<>();
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder email(String emailAddress) {
            this.email = Email.of(emailAddress);
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder addRole(Role role) {
            this.roles.add(role);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
