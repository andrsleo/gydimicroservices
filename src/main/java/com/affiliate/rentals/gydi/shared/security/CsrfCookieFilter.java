package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that ensures CSRF tokens are generated and sent to the client.
 *
 * <p>
 * This filter is essential for SPAs (like Next.js) that need to read the CSRF token
 * from a cookie before making state-changing requests (POST, PUT, PATCH, DELETE).
 * </p>
 *
 * <p>
 * <b>How it works:</b>
 * </p>
 * <ol>
 *   <li>Spring Security's CSRF filter generates a {@link CsrfToken} and stores it as a request attribute</li>
 *   <li>This filter accesses that token via {@code request.getAttribute("_csrf")}</li>
 *   <li>Calling {@code csrfToken.getToken()} forces Spring Security to generate the token if not already generated</li>
 *   <li>Spring Security's {@code CookieCsrfTokenRepository} then sets the XSRF-TOKEN cookie in the response</li>
 * </ol>
 *
 * <p>
 * <b>Why this is needed:</b>
 * </p>
 * <p>
 * By default, Spring Security only generates CSRF tokens when they're needed (lazy generation).
 * For SPAs, we need the token to be available in the cookie BEFORE the first state-changing request.
 * This filter ensures the token is generated on every request, making it immediately available to the frontend.
 * </p>
 *
 * <p>
 * <b>Integration with Next.js:</b>
 * </p>
 * <pre>{@code
 * // Backend: This filter ensures XSRF-TOKEN cookie is set
 * // Frontend: Read the cookie before making POST/PUT/PATCH/DELETE requests
 *
 * function getCsrfTokenFromCookie(): string | null {
 *   const csrfCookie = document.cookie
 *     .split('; ')
 *     .find((row) => row.startsWith('XSRF-TOKEN='));
 *   return csrfCookie ? decodeURIComponent(csrfCookie.split('=')[1]) : null;
 * }
 *
 * // Send in X-XSRF-TOKEN header
 * axios.post('/api/properties', data, {
 *   headers: { 'X-XSRF-TOKEN': getCsrfTokenFromCookie() }
 * });
 * }</pre>
 *
 * <p>
 * <b>Security Note:</b> The XSRF-TOKEN cookie is intentionally NOT httpOnly
 * (see {@code CookieCsrfTokenRepository.withHttpOnlyFalse()}) so that JavaScript
 * can read it. This is safe because the CSRF token itself is not a secret - it's
 * only effective when combined with the Same-Origin Policy.
 * </p>
 *
 * @see CsrfToken
 * @see org.springframework.security.web.csrf.CookieCsrfTokenRepository
 * @author GYDI Development Team
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

    /**
     * Ensures CSRF token is generated and sent in the response.
     *
     * <p>
     * This method runs once per request (guaranteed by {@link OncePerRequestFilter}).
     * It forces CSRF token generation by accessing the token, which triggers
     * Spring Security to set the XSRF-TOKEN cookie in the response.
     * </p>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if an error occurs during filtering
     * @throws IOException if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Get CSRF token from request attribute (set by Spring Security's CsrfFilter)
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");

        if (csrfToken != null) {
            // Force token generation by calling getToken()
            // This triggers CookieCsrfTokenRepository to set the XSRF-TOKEN cookie
            csrfToken.getToken();
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
