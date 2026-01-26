package com.affiliate.rentals.gydi.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for processing JWT tokens in requests.
 *
 * <p>
 * This filter intercepts every request to extract and validate JWT tokens,
 * setting up the security context for authenticated requests.
 * </p>
 *
 * <p>
 * <b>Profile-Aware Token Extraction:</b>
 * </p>
 * <ul>
 * <li><b>Local (SPRING_PROFILES_ACTIVE=local):</b> Extracts token ONLY from
 * Authorization header</li>
 * <li><b>Dev/Prod (SPRING_PROFILES_ACTIVE=dev/prod):</b> Extracts token ONLY
 * from httpOnly cookies</li>
 * </ul>
 *
 * <p>
 * This design ensures clean separation:
 * </p>
 * <ul>
 * <li>Local development uses simple Bearer tokens (easy debugging)</li>
 * <li>Production uses secure httpOnly cookies (XSS protection)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final Environment environment;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            TokenBlacklistService tokenBlacklistService,
            Environment environment) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // ✅ PROFILE-AWARE TOKEN EXTRACTION
        // Local: ONLY Authorization header
        // Dev/Prod: ONLY cookies
        String jwt = extractTokenBasedOnProfile(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if token is blacklisted (logged out)
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract username from JWT
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            // Token has expired - log and continue without authentication
            // Spring Security will return 401 for protected endpoints
            logger.debug("JWT token expired: {}", e.getMessage());
        } catch (JwtException e) {
            // Invalid JWT token (malformed, signature mismatch, etc.)
            logger.debug("Invalid JWT token: {}", e.getMessage());
        } catch (Exception e) {
            // Catch any other unexpected errors
            logger.error("Error processing JWT token: {}", e.getMessage());
        }

        // Always continue the filter chain
        // If authentication failed, SecurityContext will be empty and Spring Security
        // handles 401
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token with cookie-first strategy.
     *
     * <p>
     * <b>Unified Strategy (All Environments):</b>
     * </p>
     * <ol>
     * <li><b>Priority 1:</b> Try extracting from httpOnly cookie (most secure,
     * required for Next.js middleware)</li>
     * <li><b>Fallback:</b> Try extracting from Authorization header (backward
     * compatibility)</li>
     * </ol>
     *
     * <p>
     * <b>Why Cookie-First?</b>
     * </p>
     * <ul>
     * <li>Next.js middleware runs server-side and can ONLY read cookies (not
     * localStorage)</li>
     * <li>httpOnly cookies prevent XSS attacks (JavaScript cannot access them)</li>
     * <li>Consistent behavior across all environments (local, dev, prod)</li>
     * </ul>
     *
     * @param request the HTTP request
     * @return the JWT token, or null if not found
     */
    private String extractTokenBasedOnProfile(HttpServletRequest request) {
        // ✅ PRIORITY 1: Try cookie first (required for Next.js middleware)
        String token = extractTokenFromCookie(request);
        if (token != null) {
            // logger.debug("🍪 Token extracted from httpOnly cookie"); // Commented to
            // reduce log noise
            return token;
        }

        // ✅ FALLBACK: Try Authorization header (backward compatibility)
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // logger.debug("🔓 Token extracted from Authorization header (fallback)"); //
            // Commented to reduce log noise
            return token;
        }

        // logger.debug("❌ No authentication token found in cookies or Authorization
        // header"); // Commented to reduce log noise
        return null;
    }

    /**
     * Extracts token from access_token cookie.
     *
     * @param request the HTTP request
     * @return the token value, or null if not found
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
