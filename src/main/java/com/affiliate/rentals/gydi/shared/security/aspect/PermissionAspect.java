package com.affiliate.rentals.gydi.shared.security.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.shared.security.annotation.RequirePermission;
import com.affiliate.rentals.gydi.users.domain.exception.PermissionDeniedException;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Permission;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PermissionEvaluator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for enforcing permission-based access control using the {@link RequirePermission} annotation.
 *
 * <p>This aspect intercepts methods annotated with {@code @RequirePermission} and validates
 * that the currently authenticated user has the required permission before allowing method execution.</p>
 *
 * <p><strong>Permission Evaluation Flow:</strong></p>
 * <ol>
 *   <li>Extract authentication from Spring Security context</li>
 *   <li>Extract userId from JWT token</li>
 *   <li>Load complete User domain object from repository</li>
 *   <li>Use {@link PermissionEvaluator} to check permission</li>
 *   <li>Allow execution if granted, throw exception if denied</li>
 * </ol>
 *
 * <p><strong>Security Considerations:</strong></p>
 * <ul>
 *   <li>Always validates against the latest user data from the database</li>
 *   <li>Checks account verification, capabilities, and subscription plan</li>
 *   <li>ADMIN role bypasses all permission checks</li>
 *   <li>Logs all permission denials for security auditing</li>
 * </ul>
 *
 * @author GYDI Development Team
 * @see RequirePermission
 * @see PermissionEvaluator
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {
    
    private final UserRepositoryPort userRepository;
    private final JwtService jwtService;
    private final HttpServletRequest request;

    /**
     * Constructs a new PermissionAspect with required dependencies.
     *
     * @param userRepository the repository for loading user data
     * @param jwtService the service for extracting JWT claims
     * @param request the current HTTP request (for extracting JWT token)
     */
    public PermissionAspect(
            UserRepositoryPort userRepository,
            JwtService jwtService,
            HttpServletRequest request) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.request = request;
    }

    /**
     * Intercepts methods annotated with {@code @RequirePermission} and validates permissions.
     *
     * <p>This advice runs before the target method executes. If the permission check fails,
     * the target method is never executed.</p>
     *
     * @param joinPoint the join point representing the intercepted method
     * @param requirePermission the annotation containing permission requirements
     * @throws PermissionDeniedException if the user lacks the required permission
     * @throws UserNotFoundException if the authenticated user cannot be found
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        Permission requiredPermission = requirePermission.value();
        String customMessage = requirePermission.message();

        // Get current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Permission check failed: No authenticated user for permission {}", requiredPermission);
            throw PermissionDeniedException.forPermission(requiredPermission);
        }

        // Extract userId from JWT token
        Long userId = extractUserIdFromRequest();
        if (userId == null) {
            log.warn("Permission check failed: Could not extract userId from JWT for permission {}", requiredPermission);
            throw PermissionDeniedException.forPermission(requiredPermission);
        }

        // Load complete user from repository (fresh data, not from JWT)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Permission check failed: User {} not found", userId);
                    return UserNotFoundException.withId(userId);
                });

        // Evaluate permission using domain service
        boolean hasPermission = PermissionEvaluator.evaluate(user, requiredPermission);

        if (!hasPermission) {
            String reason = customMessage.isEmpty()
                    ? determineDenialReason(user, requiredPermission)
                    : customMessage;

            log.warn("Permission denied for user {} on permission {}: {}",
                    userId, requiredPermission, reason);

            // Log method details for audit
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            log.debug("Denied access to method: {}.{}",
                    signature.getDeclaringType().getSimpleName(),
                    signature.getName());

            throw PermissionDeniedException.forUser(userId, requiredPermission, reason);
        }

        // Permission granted - log for audit
        log.debug("Permission granted for user {} on permission {}", userId, requiredPermission);
    }

    /**
     * Extracts the userId from the JWT token in the current request.
     *
     * @return the userId, or null if not found
     */
    private Long extractUserIdFromRequest() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }

    /**
     * Determines the reason why a permission was denied.
     *
     * <p>This method analyzes the user's state to provide a meaningful error message.</p>
     *
     * @param user the user who was denied
     * @param permission the permission that was required
     * @return a descriptive reason for the denial
     */
    private String determineDenialReason(User user, Permission permission) {
        if (permission.requiresVerification() && !user.isAccountVerified()) {
            return "Account verification required";
        }

        return switch (permission) {
            case PROPERTY_PUBLISH -> !user.capabilities().canPublish()
                    ? "Property publishing capability is disabled"
                    : "Insufficient subscription plan for publishing";

            case REFERRAL_GENERATE -> !user.capabilities().canRefer()
                    ? "Referral generation capability is disabled"
                    : "Insufficient subscription plan for referrals";            

            case ANALYTICS_VIEW_ADVANCED, ANALYTICS_EXPORT ->
                    "Requires PRO or ELITE subscription plan";

            case USER_MANAGE ->
                    "Administrator privileges required";

            default -> "Permission denied";
        };
    }
}