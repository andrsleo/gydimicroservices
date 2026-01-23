package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Service for managing authentication cookies with profile-aware security settings.
 *
 * <p>This service provides centralized cookie management following security best practices:</p>
 * <ul>
 *   <li><b>httpOnly:</b> Prevents XSS attacks by making cookies inaccessible to JavaScript</li>
 *   <li><b>Secure:</b> HTTPS-only transmission (required for SameSite=None)</li>
 *   <li><b>SameSite:</b> Profile-dependent (Lax for dev, None for production cross-domain)</li>
 * </ul>
 *
 * <p>
 * <b>Profile-Based SameSite Policy:</b>
 * </p>
 * <ul>
 *   <li><b>Development (dev, local):</b> SameSite=Lax (sufficient for localhost, allows top-level navigation)</li>
 *   <li><b>Production (prod, production):</b> SameSite=None (required for cross-domain: Vercel → Railway)</li>
 * </ul>
 *
 * <p>
 * <b>Security Note:</b> SameSite=None requires CSRF protection. See {@code SecurityConfig}
 * for CSRF token configuration.
 * </p>
 *
 * <p>
 * <b>Spring Profiles Usage:</b> Uses {@code SPRING_PROFILES_ACTIVE} environment variable
 * to determine profile. Set to {@code dev} for development or {@code prod} for production.
 * </p>
 *
 * @author GYDI Development Team
 */
@Service
public class CookieService {

    private final Environment environment;

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int ACCESS_TOKEN_MAX_AGE = 24 * 60 * 60; // 24 hours
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

    /**
     * Constructor for dependency injection.
     *
     * @param environment Spring Environment for profile detection
     */
    public CookieService(Environment environment) {
        this.environment = environment;
    }

    /**
     * Sets authentication cookies (access token and refresh token).
     *
     * <p>Creates two httpOnly cookies with strict security settings:</p>
     * <ul>
     *   <li>access_token: 24-hour expiration</li>
     *   <li>refresh_token: 7-day expiration</li>
     * </ul>
     *
     * @param response the HTTP response
     * @param accessToken the JWT access token
     * @param refreshToken the refresh token (optional)
     */
    public void setAuthCookies(
            HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        // Set access token cookie
        Cookie accessCookie = createSecureCookie(ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_MAX_AGE);
        response.addCookie(accessCookie);

        // Set refresh token cookie if provided
        if (refreshToken != null && !refreshToken.isBlank()) {
            Cookie refreshCookie = createSecureCookie(REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_MAX_AGE);
            response.addCookie(refreshCookie);
        }
    }

    /**
     * Clears authentication cookies (logout).
     *
     * <p>Sets cookies with empty value and maxAge=0 to delete them.</p>
     *
     * @param response the HTTP response
     */
    public void clearAuthCookies(HttpServletResponse response) {
        // Clear access token
        Cookie accessCookie = createSecureCookie(ACCESS_TOKEN_COOKIE, "", 0);
        response.addCookie(accessCookie);

        // Clear refresh token
        Cookie refreshCookie = createSecureCookie(REFRESH_TOKEN_COOKIE, "", 0);
        response.addCookie(refreshCookie);
    }

    /**
     * Creates a secure cookie with environment-aware SameSite policy.
     *
     * <p><b>Security attributes:</b></p>
     * <ul>
     *   <li>httpOnly: true - Prevents XSS (JavaScript cannot read cookie)</li>
     *   <li>Secure: true - HTTPS only (required for SameSite=None)</li>
     *   <li>SameSite: Dynamic - Depends on environment (see below)</li>
     *   <li>Path: / - Available for all routes</li>
     * </ul>
     *
     * <p><b>SameSite Policy by Environment:</b></p>
     * <table border="1">
     *   <tr>
     *     <th>Environment</th>
     *     <th>SameSite Value</th>
     *     <th>Reason</th>
     *   </tr>
     *   <tr>
     *     <td>Development (localhost)</td>
     *     <td>Lax</td>
     *     <td>Same-origin requests, allows top-level navigation</td>
     *   </tr>
     *   <tr>
     *     <td>Production (Vercel → Railway)</td>
     *     <td>None</td>
     *     <td>Cross-domain requests required, CSRF tokens provide protection</td>
     *   </tr>
     * </table>
     *
     * <p>
     * <b>Why SameSite=None in Production:</b> Frontend (Vercel) and Backend (Railway)
     * are on different domains (cross-site). SameSite=Strict would block cookies from
     * being sent in cross-site requests. SameSite=None allows the frontend to send
     * cookies to the backend API.
     * </p>
     *
     * <p>
     * <b>Security Note:</b> SameSite=None requires CSRF protection. Spring Security's
     * CSRF tokens (X-XSRF-TOKEN header) provide defense against CSRF attacks. See
     * {@code SecurityConfig} for CSRF configuration.
     * </p>
     *
     * @param name the cookie name
     * @param value the cookie value
     * @param maxAge the max age in seconds
     * @return the configured cookie with environment-appropriate SameSite policy
     */
    private Cookie createSecureCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true); // XSS protection
        cookie.setSecure(true); // HTTPS only (REQUIRED with SameSite=None)
        cookie.setPath("/"); // Available for all routes
        cookie.setMaxAge(maxAge);

        // ✅ Profile-based SameSite policy
        // Development profiles (dev, local): Lax - Sufficient for same-origin
        // Production profiles (prod, production): None - Required for cross-domain with CSRF protection
        boolean isDevelopment = environment.acceptsProfiles(Profiles.of("dev", "local"));
        String sameSite = isDevelopment ? "Lax" : "None";
        cookie.setAttribute("SameSite", sameSite);

        return cookie;
    }

    /**
     * Extracts the access token from cookies.
     *
     * @param request the HTTP request
     * @return the access token, or null if not found
     */
    public String extractAccessToken(jakarta.servlet.http.HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Extracts the refresh token from cookies.
     *
     * @param request the HTTP request
     * @return the refresh token, or null if not found
     */
    public String extractRefreshToken(jakarta.servlet.http.HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    /**
     * Extracts a cookie value by name.
     *
     * @param request the HTTP request
     * @param cookieName the cookie name
     * @return the cookie value, or null if not found
     */
    private String extractCookieValue(jakarta.servlet.http.HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
