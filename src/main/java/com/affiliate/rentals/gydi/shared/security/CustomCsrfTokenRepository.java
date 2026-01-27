package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;

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

    @Override
    public void saveToken(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = (csrfToken != null) ? csrfToken.getToken() : "";

        // Determine security settings based on environment
        boolean isLocalhost = environment.acceptsProfiles(Profiles.of("local"));
        boolean isSecure = !isLocalhost; // Secure for non-local environments
        String sameSite = isLocalhost ? "Lax" : "None";

        Cookie cookie = new Cookie(CSRF_COOKIE_NAME, tokenValue);
        cookie.setPath("/");
        cookie.setHttpOnly(false); // MUST be false so JavaScript can read it
        cookie.setSecure(isSecure); // HTTPS only for cross-origin (SameSite=None requires Secure)
        cookie.setMaxAge(csrfToken != null ? -1 : 0); // Session cookie or delete

        // Build cookie header with SameSite manually (Spring Cookie class doesn't support SameSite)
        String cookieHeader = String.format(
            "%s=%s; Path=/; HttpOnly=%s; Secure=%s; SameSite=%s",
            CSRF_COOKIE_NAME,
            tokenValue,
            false, // Must be false for JavaScript to read
            isSecure,
            sameSite
        );

        response.addHeader("Set-Cookie", cookieHeader);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, token);
                    }
                }
            }
        }
        return null;
    }

    private String createNewToken() {
        return UUID.randomUUID().toString();
    }
}
