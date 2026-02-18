package com.affiliate.rentals.gydi.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * Custom implementation of UserDetails that includes user ID.
 * <p>
 * This allows extracting the user ID directly from the SecurityContext
 * without needing to parse the JWT token on every request.
 * </p>
 * <p>
 * <strong>Security Note:</strong> This class is immutable and thread-safe.
 * The user ID is set during authentication and cannot be modified afterwards.
 * </p>
 *
 * @author GYDI Development Team
 */
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountVerified;
    private final boolean enabled;

    /**
     * Creates a new CustomUserDetails instance.
     *
     * @param userId the user's ID
     * @param email the user's email address (used as username)
     * @param password the user's encoded password
     * @param authorities the user's granted authorities (roles)
     * @param accountVerified whether the user's account is verified
     * @param enabled whether the user's account is enabled
     */
    public CustomUserDetails(
        Long userId,
        String email,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        boolean accountVerified,
        boolean enabled
    ) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.authorities = Objects.requireNonNull(authorities, "Authorities cannot be null");
        this.accountVerified = accountVerified;
        this.enabled = enabled;
    }

    /**
     * Returns the user's ID.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the user's email (used as username).
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns whether the user's account is verified.
     *
     * @return true if account is verified, false otherwise
     */
    public boolean isAccountVerified() {
        return accountVerified;
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role name (without "ROLE_" prefix, e.g., "ADMIN")
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return authorities.stream()
            .anyMatch(authority ->
                authority.getAuthority().equals("ROLE_" + role) ||
                authority.getAuthority().equals(role)
            );
    }

    // ========== UserDetails Interface Implementation ==========

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomUserDetails that = (CustomUserDetails) o;
        return Objects.equals(userId, that.userId) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    @Override
    public String toString() {
        return String.format(
            "CustomUserDetails{userId=%d, email='%s', verified=%b, enabled=%b, authorities=%s}",
            userId, email, accountVerified, enabled, authorities
        );
    }
}
