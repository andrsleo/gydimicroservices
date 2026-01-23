package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Custom CSRF Token Request Handler for Single Page Applications (SPAs).
 *
 * <p>
 * This handler is specifically designed for SPA architectures like Next.js
 * where CSRF tokens need to be sent in the request header (X-XSRF-TOKEN)
 * rather than as a form parameter.
 * </p>
 *
 * <p>
 * <b>How it works:</b>
 * </p>
 * <ol>
 *   <li>Uses {@link XorCsrfTokenRequestAttributeHandler} internally for token generation</li>
 *   <li>Accepts CSRF tokens from the {@code X-XSRF-TOKEN} header (SPA standard)</li>
 *   <li>Validates token using XOR-based comparison (prevents breach attacks)</li>
 * </ol>
 *
 * <p>
 * <b>Security Note:</b> This implementation uses XOR encoding for CSRF tokens,
 * which provides protection against BREACH attacks by ensuring tokens are different
 * on each request while remaining cryptographically valid.
 * </p>
 *
 * <p>
 * <b>Integration with Next.js:</b>
 * </p>
 * <pre>{@code
 * // Frontend (Next.js API client)
 * const csrfToken = getCsrfTokenFromCookie(); // Read XSRF-TOKEN cookie
 * axios.post('/api/properties', data, {
 *   headers: { 'X-XSRF-TOKEN': csrfToken }
 * });
 * }</pre>
 *
 * @see CsrfTokenRequestAttributeHandler
 * @see XorCsrfTokenRequestAttributeHandler
 * @author GYDI Development Team
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    /**
     * Handles CSRF token generation and makes it available to the request.
     *
     * <p>
     * Delegates to {@link XorCsrfTokenRequestAttributeHandler} to generate
     * the XOR-encoded CSRF token and make it available as a request attribute.
     * </p>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param csrfToken supplier for the CSRF token
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {
        // Delegate to XorCsrfTokenRequestAttributeHandler for token generation
        this.delegate.handle(request, response, csrfToken);
    }

    /**
     * Resolves the CSRF token value from the request.
     *
     * <p>
     * <b>Resolution Strategy:</b>
     * </p>
     * <ol>
     *   <li>First, check if token is present in the {@code X-XSRF-TOKEN} header (SPA approach)</li>
     *   <li>If found, use the parent class's resolution logic</li>
     *   <li>If not found, delegate to {@link XorCsrfTokenRequestAttributeHandler}</li>
     * </ol>
     *
     * <p>
     * This allows the handler to accept tokens from both:
     * </p>
     * <ul>
     *   <li>Request header: {@code X-XSRF-TOKEN} (SPAs like Next.js)</li>
     *   <li>Form parameter: {@code _csrf} (traditional server-rendered forms)</li>
     * </ul>
     *
     * @param request the HTTP request
     * @param csrfToken the CSRF token configuration
     * @return the resolved CSRF token value from the request
     */
    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Check if CSRF token is present in the header (SPA approach)
        String headerName = csrfToken.getHeaderName();
        String headerValue = request.getHeader(headerName);

        if (StringUtils.hasText(headerValue)) {
            // Token found in header - use parent's resolution logic
            return super.resolveCsrfTokenValue(request, csrfToken);
        }

        // Token not in header - delegate to XorCsrfTokenRequestAttributeHandler
        // This handles form-based CSRF tokens (traditional approach)
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
