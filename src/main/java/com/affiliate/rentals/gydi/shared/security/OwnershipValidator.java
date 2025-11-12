package com.affiliate.rentals.gydi.shared.security;

import com.affiliate.rentals.gydi.shared.exception.ForbiddenException;
import com.affiliate.rentals.gydi.users.domain.model.Email;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for validating resource ownership to prevent IDOR vulnerabilities.
 *
 * <p><b>SECURITY: Insecure Direct Object Reference (IDOR) Prevention</b></p>
 *
 * <p>This service validates that authenticated users can only access their own resources
 * unless they have ADMIN role. This prevents IDOR attacks where a user could access
 * another user's data by simply changing IDs in the URL.</p>
 *
 * <p>Example IDOR vulnerability:</p>
 * <pre>
 * GET /api/users/1/profile  ← User A's profile
 * GET /api/users/2/profile  ← User A can access User B's profile (IDOR!)
 * </pre>
 *
 * <p>This validator ensures:</p>
 * <ul>
 *   <li>Users can only access their own resources (userId must match)</li>
 *   <li>ADMIN users can access any resource</li>
 *   <li>Unauthenticated requests are rejected</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnershipValidator {

    private final UserRepositoryPort userRepository;

    /**
     * Validates that the authenticated user owns the resource or is an ADMIN.
     *
     * @param resourceOwnerId the ID of the user who owns the resource
     * @throws ForbiddenException if the user doesn't own the resource and is not ADMIN
     * @throws IllegalStateException if no user is authenticated
     */
    public void validateOwnership(Long resourceOwnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("SECURITY: Attempted access to resource without authentication. Resource owner ID: {}",
                    resourceOwnerId);
            throw new IllegalStateException("User not authenticated");
        }

        // ADMIN users can access any resource
        if (hasRole(authentication, "ROLE_ADMIN")) {
            log.debug("SECURITY: ADMIN user accessing resource. Resource owner ID: {}", resourceOwnerId);
            return;
        }

        // Get authenticated user's email
        String authenticatedEmail = authentication.getName();

        // Get authenticated user's ID from database
        Long authenticatedUserId = getUserIdByEmail(authenticatedEmail);

        // Check if user owns the resource
        if (!authenticatedUserId.equals(resourceOwnerId)) {
            log.warn("SECURITY: IDOR attempt detected! User {} (ID: {}) tried to access resource owned by user ID: {}",
                    authenticatedEmail, authenticatedUserId, resourceOwnerId);
            throw new ForbiddenException(
                    "Access denied. You do not have permission to access this resource."
            );
        }

        log.debug("SECURITY: Ownership validated. User {} (ID: {}) accessing own resource",
                authenticatedEmail, authenticatedUserId);
    }

    /**
     * Gets the authenticated user's ID.
     *
     * @return the ID of the currently authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        String authenticatedEmail = authentication.getName();
        return getUserIdByEmail(authenticatedEmail);
    }

    /**
     * Checks if the authenticated user is an ADMIN.
     *
     * @return true if user has ADMIN role, false otherwise
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return hasRole(authentication, "ROLE_ADMIN");
    }

    /**
     * Checks if the authenticated user owns the resource or is an ADMIN.
     *
     * @param resourceOwnerId the ID of the resource owner
     * @return true if user owns resource or is ADMIN, false otherwise
     */
    public boolean canAccess(Long resourceOwnerId) {
        try {
            validateOwnership(resourceOwnerId);
            return true;
        } catch (ForbiddenException | IllegalStateException e) {
            return false;
        }
    }

    /**
     * Gets user ID by email address.
     *
     * @param email the user's email
     * @return the user ID
     * @throws IllegalStateException if user not found
     */
    private Long getUserIdByEmail(String email) {
        Email emailObj = Email.of(email);
        User user = userRepository.findByEmail(emailObj)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        return user.id();
    }

    /**
     * Checks if authentication has a specific role.
     *
     * @param authentication the authentication object
     * @param role the role to check
     * @return true if role is present, false otherwise
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }
}