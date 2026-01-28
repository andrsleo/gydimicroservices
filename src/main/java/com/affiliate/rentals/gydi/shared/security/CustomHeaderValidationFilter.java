package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filter that validates the presence of {@code X-Requested-With: XMLHttpRequest} header
 * on state-changing API requests (POST, PUT, PATCH, DELETE).
 *
 * <p>
 * <b>Defense in Depth (Option D):</b> This filter complements CSRF token validation
 * by requiring a custom header that cannot be set by simple HTML forms or cross-origin
 * requests without a CORS preflight. Browsers enforce that custom headers trigger a
 * preflight OPTIONS request, which CORS configuration must approve.
 * </p>
 *
 * <p>
 * <b>Why this helps:</b>
 * </p>
 * <ul>
 *   <li>HTML forms cannot set custom headers (only standard Content-Type values)</li>
 *   <li>Cross-origin JavaScript requests with custom headers require CORS preflight</li>
 *   <li>Combined with CSRF tokens, provides two independent layers of protection</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public final class CustomHeaderValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomHeaderValidationFilter.class);

    private static final String CUSTOM_HEADER = "X-Requested-With";
    private static final String EXPECTED_VALUE = "XMLHttpRequest";

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/users/register",
            "/api/v1/referrals/resolve"
    );

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only filter API requests
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();

        // Skip safe (read-only) methods
        if (SAFE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Skip exempt paths (authentication endpoints)
        boolean isExempt = EXEMPT_PATHS.stream().anyMatch(path::startsWith);
        if (isExempt) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate X-Requested-With header
        String headerValue = request.getHeader(CUSTOM_HEADER);
        if (!EXPECTED_VALUE.equals(headerValue)) {
            log.warn("Rejected {} {} - Missing or invalid {} header (value: {})",
                    method, path, CUSTOM_HEADER, headerValue);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Missing or invalid X-Requested-With header\",\"status\":403}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
