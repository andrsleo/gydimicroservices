package com.affiliate.rentals.gydi.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * No-operation CSRF authentication strategy that prevents token rotation.
 *
 * <p>
 * In cross-origin SPA architectures (Vercel frontend → Railway backend), CSRF token
 * rotation after authentication causes critical race conditions:
 * </p>
 * <ol>
 *   <li>Frontend fetches CSRF token (XOR-encoded based on cookie value X)</li>
 *   <li>CsrfAuthenticationStrategy rotates token → cookie changes to Y</li>
 *   <li>Frontend sends request with token based on X, but cookie is now Y → MISMATCH</li>
 *   <li>Request fails with 403 Forbidden</li>
 * </ol>
 *
 * <p>
 * <b>Solution:</b> This strategy does nothing, keeping the CSRF token stable throughout
 * the session lifetime. The CSRF token is still validated on every state-changing request,
 * maintaining security. Only the automatic rotation after authentication is disabled.
 * </p>
 *
 * <p>
 * <b>Security Analysis:</b>
 * </p>
 * <ul>
 *   <li>CSRF protection still active (token required for POST/PUT/PATCH/DELETE)</li>
 *   <li>X-Requested-With header validation still active (defense in depth)</li>
 *   <li>Token rotation was designed for session fixation attacks, which don't apply
 *       to stateless JWT authentication</li>
 *   <li>Same security guarantees as standard CSRF, without cross-origin timing issues</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
public final class NoOpCsrfAuthenticationStrategy implements SessionAuthenticationStrategy {

    private static final Logger log = LoggerFactory.getLogger(NoOpCsrfAuthenticationStrategy.class);

    @Override
    public void onAuthentication(Authentication authentication,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        // Intentionally do nothing - prevent CSRF token rotation
        // This fixes cross-origin race conditions in SPA architectures
        log.debug("NoOpCsrfAuthenticationStrategy - Skipping CSRF token rotation for {} {}",
                request.getMethod(), request.getRequestURI());
    }
}
