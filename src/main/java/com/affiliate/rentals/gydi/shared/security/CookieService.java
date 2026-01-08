package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

/**
 * Service for managing authentication cookies.
 *
 * <p>This service provides centralized cookie management following security best practices:</p>
 * <ul>
 *   <li><b>httpOnly:</b> Prevents XSS attacks by making cookies inaccessible to JavaScript</li>
 *   <li><b>Secure:</b> HTTPS-only transmission (production)</li>
 *   <li><b>SameSite=Strict:</b> CSRF protection by preventing cross-site requests</li>
 * </ul>
 *
 * <p>Used for production environment where tokens are stored in httpOnly cookies
 * instead of localStorage (XSS-proof authentication).</p>
 *
 * @author GYDI Development Team
 */
@Service
public class CookieService {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int ACCESS_TOKEN_MAX_AGE = 24 * 60 * 60; // 24 hours
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

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
     * Creates a secure cookie with security best practices.
     *
     * <p>Security attributes:</p>
     * <ul>
     *   <li>httpOnly: true - Prevents XSS</li>
     *   <li>Secure: true - HTTPS only</li>
     *   <li>SameSite: Strict - CSRF protection</li>
     *   <li>Path: / - Available for all routes</li>
     * </ul>
     *
     * @param name the cookie name
     * @param value the cookie value
     * @param maxAge the max age in seconds
     * @return the configured cookie
     */
    private Cookie createSecureCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true); // XSS protection
        cookie.setSecure(true); // HTTPS only in production
        cookie.setPath("/"); // Available for all routes
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict"); // CSRF protection

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
