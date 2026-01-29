package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Diagnostic filter for CSRF troubleshooting in production.
 *
 * <p>
 * This filter runs BEFORE Spring Security's CsrfFilter to log all incoming
 * request information related to CSRF validation. This helps diagnose 403
 * errors that occur during CSRF validation.
 * </p>
 *
 * <p>
 * <b>What it logs:</b>
 * </p>
 * <ul>
 *   <li>Request method and path</li>
 *   <li>All cookies present (names only)</li>
 *   <li>Whether XSRF-TOKEN cookie is present</li>
 *   <li>Whether X-XSRF-TOKEN header is present</li>
 *   <li>Whether X-Requested-With header is present</li>
 *   <li>Origin header (for CORS debugging)</li>
 * </ul>
 *
 * <p>
 * <b>Important:</b> This filter should be placed BEFORE CsrfFilter in the filter chain
 * so it can log even when CSRF validation fails.
 * </p>
 *
 * @author GYDI Development Team
 */
public final class CsrfDiagnosticFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfDiagnosticFilter.class);

    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();

        // Only log for API requests with state-changing methods
        if (path.startsWith("/api/") && STATE_CHANGING_METHODS.contains(method)) {
            logCsrfDiagnostics(request, method, path);
        }

        filterChain.doFilter(request, response);
    }

    private void logCsrfDiagnostics(HttpServletRequest request, String method, String path) {
        // Get cookie information
        Cookie[] cookies = request.getCookies();
        int cookieCount = cookies != null ? cookies.length : 0;
        boolean hasXsrfCookie = false;
        String xsrfCookiePrefix = "null";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("XSRF-TOKEN".equals(cookie.getName())) {
                    hasXsrfCookie = true;
                    String value = cookie.getValue();
                    xsrfCookiePrefix = value != null && value.length() > 8
                            ? value.substring(0, 8) + "..."
                            : value;
                    break;
                }
            }
        }

        // Get header information
        String xsrfHeader = request.getHeader("X-XSRF-TOKEN");
        boolean hasXsrfHeader = xsrfHeader != null && !xsrfHeader.isEmpty();
        int xsrfHeaderLength = xsrfHeader != null ? xsrfHeader.length() : 0;

        String requestedWith = request.getHeader("X-Requested-With");
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        // Log comprehensive diagnostic information
        log.info("🔍 CSRF DIAGNOSTIC - {} {} | " +
                        "Cookies: {} (XSRF-TOKEN: {}, prefix: {}) | " +
                        "X-XSRF-TOKEN header: {} (len: {}) | " +
                        "X-Requested-With: {} | " +
                        "Origin: {} | " +
                        "Referer: {}",
                method, path,
                cookieCount, hasXsrfCookie, xsrfCookiePrefix,
                hasXsrfHeader, xsrfHeaderLength,
                requestedWith,
                origin,
                referer);

        // Log all cookie names for debugging
        if (cookies != null && cookies.length > 0) {
            String cookieNames = Arrays.stream(cookies)
                    .map(Cookie::getName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            log.info("🔍 CSRF DIAGNOSTIC - Cookie names: [{}]", cookieNames);
        } else {
            log.warn("⚠️ CSRF DIAGNOSTIC - NO COOKIES received for {} {}", method, path);
        }

        // Warn about potential issues
        if (!hasXsrfCookie && !hasXsrfHeader) {
            log.warn("⚠️ CSRF DIAGNOSTIC - Neither XSRF-TOKEN cookie nor X-XSRF-TOKEN header present. " +
                    "This request WILL fail CSRF validation.");
        } else if (!hasXsrfCookie && hasXsrfHeader) {
            log.warn("⚠️ CSRF DIAGNOSTIC - X-XSRF-TOKEN header present but NO XSRF-TOKEN cookie. " +
                    "This may indicate third-party cookie blocking. Request may fail CSRF validation.");
        }
    }
}
