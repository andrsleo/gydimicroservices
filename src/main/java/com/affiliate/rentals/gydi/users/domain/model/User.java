package com.affiliate.rentals.gydi.users.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * User domain entity representing a user in the rental system.
 *
 * <p>This is an immutable domain entity following Domain-Driven Design (DDD) principles.
 * It encapsulates all user-related data including authentication credentials, roles, and metadata.
 * The class uses the Builder pattern for flexible object construction and ensures all business
 * rules are enforced through validation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * User user = User.builder()
 *     .name("John Doe")
 *     .email("john.doe@example.com")
 *     .passwordHash("hashed_password")
 *     .addRole(Role.guest())
 *     .build();
 * }</pre>
 *
 * @author GYDI Development Team
 * @see Email
 * @see Role
 * @see RoleName
 */
public final class User {
    private final Long id;
    private final String name;
    private final Email email;
    private final String passwordHash;
    private final String phoneNumber;
    private final Set<Role> roles;
    private final SubscriptionPlan activePlan;
    private final UserCapabilities capabilities;
    private final boolean accountVerified;
    private final boolean verifiedCreator;
    private final LocalDateTime createdAt;

    /**
     * Private constructor used by the Builder.
     * Validates all required fields and creates an immutable User instance.
     *
     * @param builder the builder instance containing all user data
     * @throws NullPointerException if any required field is null
     */
    private User(Builder builder) {
        this.id = builder.id;
        this.name = Objects.requireNonNull(builder.name, "Name cannot be null");
        this.email = Objects.requireNonNull(builder.email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "Password hash cannot be null");
        this.phoneNumber = builder.phoneNumber;
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.activePlan = Objects.requireNonNullElse(builder.activePlan, SubscriptionPlan.FREE);
        this.capabilities = Objects.requireNonNullElse(builder.capabilities, UserCapabilities.defaultCapabilities());
        this.accountVerified = builder.accountVerified;
        this.verifiedCreator = builder.verifiedCreator;
        this.createdAt = Objects.requireNonNullElseGet(builder.createdAt, LocalDateTime::now);
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return the user ID, may be {@code null} for new users not yet persisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the user's display name.
     *
     * @return the user's name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the user's email address.
     *
     * @return the validated email address, never {@code null}
     */
    public Email email() {
        return email;
    }

    /**
     * Returns the user's hashed password.
     *
     * @return the password hash, never {@code null}
     */
    public String passwordHash() {
        return passwordHash;
    }

    /**
     * Returns the user's phone number.
     *
     * @return the phone number, may be {@code null}
     */
    public String phoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns an unmodifiable set of the user's roles.
     *
     * @return the user's roles as an unmodifiable set, never {@code null}
     */
    public Set<Role> roles() {
        return roles;
    }

    /**
     * Returns the timestamp when this user was created.
     *
     * @return the creation timestamp, never {@code null}
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * Returns the user's active subscription plan.
     *
     * @return the active subscription plan, never {@code null}
     */
    public SubscriptionPlan activePlan() {
        return activePlan;
    }

    /**
     * Returns the user's capabilities.
     *
     * @return the user capabilities, never {@code null}
     */
    public UserCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Returns whether the user's account is verified.
     *
     * @return {@code true} if account is verified, {@code false} otherwise
     */
    public boolean isAccountVerified() {
        return accountVerified;
    }

    /**
     * Returns whether this user has passed creator verification (UGC Social Commerce).
     * Required gate for submitting collaboration pitches.
     */
    public boolean isVerifiedCreator() {
        return verifiedCreator;
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param roleName the role name to check
     * @return {@code true} if the user has the specified role, {@code false} otherwise
     */
    public boolean hasRole(RoleName roleName) {
        return roles.stream()
                .anyMatch(role -> role.name().equals(roleName));
    }

    /**
     * Checks if the user is an admin.
     *
     * @return {@code true} if user has ADMIN role, {@code false} otherwise
     */
    public boolean isAdmin() {
        return hasRole(RoleName.ADMIN);
    }

    /**
     * Creates a new User instance with an additional role.
     * This method maintains immutability by returning a new instance.
     *
     * @param role the role to add
     * @return a new User instance with the added role
     * @throws NullPointerException if role is {@code null}
     */
    public User addRole(Role role) {
        var updatedRoles = new HashSet<>(this.roles);
        updatedRoles.add(role);
        return User.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .passwordHash(this.passwordHash)
                .phoneNumber(this.phoneNumber)
                .roles(updatedRoles)
                .activePlan(this.activePlan)
                .capabilities(this.capabilities)
                .accountVerified(this.accountVerified)
                .verifiedCreator(this.verifiedCreator)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * Creates a new User instance with a role removed.
     * This method maintains immutability by returning a new instance.
     *
     * @param role the role to remove
     * @return a new User instance without the specified role
     * @throws NullPointerException if role is {@code null}
     */
    public User removeRole(Role role) {
        var updatedRoles = new HashSet<>(this.roles);
        updatedRoles.remove(role);
        return User.builder()
                .id(this.id)
                .name(this.name)
                .email(this.email)
                .passwordHash(this.passwordHash)
                .phoneNumber(this.phoneNumber)
                .roles(updatedRoles)
                .activePlan(this.activePlan)
                .capabilities(this.capabilities)
                .accountVerified(this.accountVerified)
                .verifiedCreator(this.verifiedCreator)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * Creates a new Builder instance for constructing User objects.
     *
     * @return a new Builder instance
     */
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

    /**
     * Builder class for constructing {@link User} instances.
     *
     * <p>This builder follows the fluent interface pattern and provides
     * methods for setting all user properties. It validates required fields
     * when {@link #build()} is called.</p>
     */
    public static final class Builder {
        private Long id;
        private String name;
        private Email email;
        private String passwordHash;
        private String phoneNumber;
        private Set<Role> roles = new HashSet<>();
        private SubscriptionPlan activePlan;
        private UserCapabilities capabilities;
        private boolean accountVerified;
        private boolean verifiedCreator;
        private LocalDateTime createdAt;

        private Builder() {
        }

        /**
         * Sets the user ID.
         *
         * @param id the user ID
         * @return this builder instance
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the user's display name.
         *
         * @param name the user's name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the user's email using an {@link Email} value object.
         *
         * @param email the email value object
         * @return this builder instance
         */
        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the user's email using a string address.
         * The address will be validated when creating the {@link Email} value object.
         *
         * @param emailAddress the email address string
         * @return this builder instance
         * @throws IllegalArgumentException if the email format is invalid
         */
        public Builder email(String emailAddress) {
            this.email = Email.of(emailAddress);
            return this;
        }

        /**
         * Sets the user's hashed password.
         *
         * @param passwordHash the hashed password
         * @return this builder instance
         */
        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        /**
         * Sets the user's phone number.
         *
         * @param phoneNumber the phone number
         * @return this builder instance
         */
        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        /**
         * Sets the user's roles as a set.
         *
         * @param roles the set of roles
         * @return this builder instance
         */
        public Builder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * Adds a single role to the user's role set.
         *
         * @param role the role to add
         * @return this builder instance
         */
        public Builder addRole(Role role) {
            this.roles.add(role);
            return this;
        }

        /**
         * Sets the user's active subscription plan.
         *
         * @param activePlan the subscription plan
         * @return this builder instance
         */
        public Builder activePlan(SubscriptionPlan activePlan) {
            this.activePlan = activePlan;
            return this;
        }

        /**
         * Sets the user's capabilities.
         *
         * @param capabilities the user capabilities
         * @return this builder instance
         */
        public Builder capabilities(UserCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        /**
         * Sets whether the user's account is verified.
         *
         * @param accountVerified the verification status
         * @return this builder instance
         */
        public Builder accountVerified(boolean accountVerified) {
            this.accountVerified = accountVerified;
            return this;
        }

        public Builder verifiedCreator(boolean verifiedCreator) {
            this.verifiedCreator = verifiedCreator;
            return this;
        }

        /**
         * Sets the user's creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds and returns a new {@link User} instance.
         *
         * @return the constructed User
         * @throws NullPointerException if any required field is null
         */
        public User build() {
            return new User(this);
        }
    }
}
