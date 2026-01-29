package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * Custom CSRF Token Repository that sets cookie with correct SameSite attribute for production.
 *
 * <p>
 * Spring Security's default CookieCsrfTokenRepository doesn't support SameSite=None for cross-origin requests.
 * This custom implementation ensures XSRF-TOKEN cookie works in production (Vercel → Railway).
 * </p>
 *
 * <p><b>Cookie Configuration by Profile:</b></p>
 * <ul>
 *   <li><b>Local:</b> SameSite=Lax, Secure=false</li>
 *   <li><b>Dev/Prod:</b> SameSite=None, Secure=true (required for cross-origin)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public class CustomCsrfTokenRepository implements CsrfTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomCsrfTokenRepository.class);

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAMETER_NAME = "_csrf";

    private final Environment environment;

    public CustomCsrfTokenRepository(Environment environment) {
        this.environment = environment;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, createNewToken());
    }

    /**
     * Request attribute name for storing the raw CSRF token value.
     * Used by AuthController to return the token in the response body
     * for cross-origin frontends that cannot read the XSRF-TOKEN cookie.
     */
    public static final String CSRF_RAW_TOKEN_ATTR = "CSRF_RAW_TOKEN";

    @Override
    public void saveToken(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = (csrfToken != null) ? csrfToken.getToken() : "";
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Store raw token as request attribute for controller access (cross-origin support)
        if (csrfToken != null) {
            request.setAttribute(CSRF_RAW_TOKEN_ATTR, tokenValue);
        }

        // Determine security settings based on environment
        boolean isLocalhost = environment.acceptsProfiles(Profiles.of("local"));
        boolean isSecure = !isLocalhost; // Secure for non-local environments
        String sameSite = isLocalhost ? "Lax" : "None";

        // Use Spring's ResponseCookie for proper SameSite support
        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE_NAME, tokenValue)
            .path("/")
            .httpOnly(false) // MUST be false so JavaScript can read it
            .secure(isSecure) // HTTPS only for cross-origin (SameSite=None requires Secure)
            .sameSite(sameSite)
            .maxAge(csrfToken != null ? Duration.ofDays(1) : Duration.ZERO) // 1 day or delete
            .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("CSRF saveToken - {} {} - Set XSRF-TOKEN cookie (SameSite={}, Secure={}, token prefix: {})",
                method, path, sameSite, isSecure,
                tokenValue.length() > 0 ? tokenValue.substring(0, Math.min(8, tokenValue.length())) : "empty");
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        // Store raw token as request attribute for controller access
                        request.setAttribute(CSRF_RAW_TOKEN_ATTR, token);
                        log.debug("CSRF loadToken - {} {} - Found XSRF-TOKEN cookie, token prefix: {}",
                                method, path, token.substring(0, Math.min(8, token.length())));
                        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, token);
                    }
                }
            }
        }
        log.debug("CSRF loadToken - {} {} - NO XSRF-TOKEN cookie found (cookies count: {})",
                method, path, cookies != null ? cookies.length : 0);
        return null;
    }

    private String createNewToken() {
        return UUID.randomUUID().toString();
    }
}
