package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

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
 *   <li><b>Local (localhost):</b> SameSite=Lax (same-origin: localhost:3000 → localhost:8080)</li>
 *   <li><b>Railway Dev:</b> SameSite=None (cross-domain: Vercel → Railway Dev, requires CSRF)</li>
 *   <li><b>Railway Production:</b> SameSite=None (cross-domain: Vercel → Railway Prod, requires CSRF)</li>
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

    private static final Logger log = LoggerFactory.getLogger(CookieService.class);

    private final Environment environment;
    private final JwtService jwtService;

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /**
     * Constructor for dependency injection.
     *
     * @param environment Spring Environment for profile detection
     * @param jwtService JWT service for token expiration info
     */
    public CookieService(Environment environment, JwtService jwtService) {
        this.environment = environment;
        this.jwtService = jwtService;
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
        // Detect if request is over HTTPS (direct or via proxy like Railway/Vercel)
        boolean isSecure = isHttpsRequest(response);
        boolean isLocalhost = environment.acceptsProfiles(Profiles.of("local"));
        String sameSite = isLocalhost ? "Lax" : "None";

        // ✅ INACTIVITY TIMEOUT SYNC: Get token expiration from JwtService configuration
        // This ensures cookie max-age matches JWT expiration (now 1 hour instead of 24h)
        long accessTokenMaxAgeSeconds = jwtService.getAccessTokenExpirationInSeconds();
        long refreshTokenMaxAgeSeconds = jwtService.getRefreshExpirationInSeconds();

        log.info("🍪 Setting auth cookies - Profile: {}, Secure: {}, SameSite: {}, Access Token Max-Age: {}s ({}h)",
                 environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default",
                 isSecure, sameSite, accessTokenMaxAgeSeconds, accessTokenMaxAgeSeconds / 3600);

        // Set access token cookie using Spring's ResponseCookie for proper SameSite support
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .path("/")
                .httpOnly(true) // XSS protection
                .secure(isSecure) // HTTPS only (required for SameSite=None)
                .sameSite(sameSite)
                .maxAge(Duration.ofSeconds(accessTokenMaxAgeSeconds))
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        log.info("✅ Cookie set: {} (maxAge={}s, Secure={}, SameSite={})",
                 ACCESS_TOKEN_COOKIE, accessTokenMaxAgeSeconds, isSecure, sameSite);

        // Set refresh token cookie if provided
        if (refreshToken != null && !refreshToken.isBlank()) {
            ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                    .path("/")
                    .httpOnly(true) // XSS protection
                    .secure(isSecure) // HTTPS only (required for SameSite=None)
                    .sameSite(sameSite)
                    .maxAge(Duration.ofSeconds(refreshTokenMaxAgeSeconds))
                .build();

            response.addHeader("Set-Cookie", refreshCookie.toString());
            log.info("✅ Cookie set: {} (maxAge={}s, Secure={}, SameSite={})",
                     REFRESH_TOKEN_COOKIE, refreshTokenMaxAgeSeconds, isSecure, sameSite);
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
        boolean isSecure = isHttpsRequest(response);
        boolean isLocalhost = environment.acceptsProfiles(Profiles.of("local"));
        String sameSite = isLocalhost ? "Lax" : "None";

        // Clear access token using ResponseCookie
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(Duration.ZERO) // Delete cookie
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        // Clear refresh token using ResponseCookie
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .maxAge(Duration.ZERO) // Delete cookie
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
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

    /**
     * Determines if cookies should have the Secure flag based on environment.
     *
     * <p>
     * <b>Logic:</b>
     * </p>
     * <ul>
     *   <li><b>local profile:</b> false (HTTP on localhost:8080)</li>
     *   <li><b>dev/prod profiles:</b> true (Railway uses HTTPS externally, even if internal is HTTP)</li>
     * </ul>
     *
     * <p>
     * <b>Railway/Vercel Architecture:</b> These platforms use HTTPS terminiation at the edge proxy,
     * but the internal connection to the application is HTTP. The Secure flag must be set to true
     * because the browser sees HTTPS, even though Tomcat runs on HTTP internally.
     * </p>
     *
     * @param response the HTTP response (not used, kept for potential future header inspection)
     * @return true if Secure flag should be set, false otherwise
     */
    private boolean isHttpsRequest(HttpServletResponse response) {
        // Local profile = HTTP (localhost development)
        // Dev/Prod profiles = HTTPS (Railway always serves HTTPS externally)
        return !environment.acceptsProfiles(Profiles.of("local"));
    }
}
