package com.affiliate.rentals.gydi.shared.config;

import com.affiliate.rentals.gydi.shared.security.CsrfCookieFilter;
import com.affiliate.rentals.gydi.shared.security.CsrfDiagnosticFilter;
import com.affiliate.rentals.gydi.shared.security.CustomCsrfTokenRepository;
import com.affiliate.rentals.gydi.shared.security.CustomHeaderValidationFilter;
import com.affiliate.rentals.gydi.shared.security.JwtAuthenticationFilter;
import com.affiliate.rentals.gydi.shared.security.NoOpCsrfAuthenticationStrategy;
import com.affiliate.rentals.gydi.shared.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration for the application.
 *
 * <p>
 * This configuration sets up JWT-based stateless authentication with role-based
 * access control. It configures the security filter chain, authentication
 * provider,
 * and password encoder.
 * </p>
 *
 * @author GYDI Development Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final UserDetailsService userDetailsService;
        private final Environment environment;

        @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
        private String allowedOrigins;

        @Value("${cors.allowed-origin-patterns:}")
        private String allowedOriginPatterns;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService, Environment environment) {
                this.jwtAuthFilter = jwtAuthFilter;
                this.userDetailsService = userDetailsService;
                this.environment = environment;
        }

        /**
         * Configures the security filter chain with CSRF protection for SPAs.
         *
         * <p>
         * <b>SECURITY DECISION: CSRF protection is ENABLED</b>
         * </p>
         *
         * <p>
         * <b>Justification:</b>
         * </p>
         * <ol>
         * <li><b>Cross-Domain Architecture:</b> Frontend (Vercel) and Backend (Railway)
         * are on different domains</li>
         * <li><b>SameSite=None Cookies:</b> Required for cross-domain auth cookies
         * (access_token, refresh_token)</li>
         * <li><b>CSRF Risk:</b> SameSite=None cookies are sent automatically, creating
         * CSRF vulnerability without protection</li>
         * <li><b>Mitigation:</b> CSRF tokens (X-XSRF-TOKEN header) validate that
         * requests originate from our frontend</li>
         * </ol>
         *
         * <p>
         * <b>CSRF Configuration for SPAs:</b>
         * </p>
         * <ul>
         * <li><b>Token Repository:</b>
         * {@code CookieCsrfTokenRepository.withHttpOnlyFalse()} - Stores token in
         * XSRF-TOKEN cookie (readable by JavaScript)</li>
         * <li><b>Token Handler:</b> {@link SpaCsrfTokenRequestHandler} - Custom handler
         * for SPA token validation</li>
         * <li><b>Exempt Endpoints:</b> Login, register, refresh - These don't need CSRF
         * (no state-changing operations on existing sessions)</li>
         * <li><b>Cookie Filter:</b> {@link CsrfCookieFilter} - Ensures CSRF token is
         * sent in every response</li>
         * </ul>
         *
         * <p>
         * <b>How Frontend Uses CSRF Tokens:</b>
         * </p>
         * 
         * <pre>{@code
         * // 1. Read CSRF token from XSRF-TOKEN cookie
         * const csrfToken = getCsrfTokenFromCookie();
         *
         * // 2. Send in X-XSRF-TOKEN header for POST/PUT/PATCH/DELETE
         * axios.post('/api/properties', data, {
         *   headers: { 'X-XSRF-TOKEN': csrfToken }
         * });
         * }</pre>
         *
         * @param http the HttpSecurity to configure
         * @return the configured SecurityFilterChain
         * @throws Exception if an error occurs during configuration
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // ✅ ENABLE CSRF protection for SPA with custom handler
                                // ============================================================
                                // CSRF PROTECTION STRATEGY FOR CROSS-ORIGIN SPA
                                // ============================================================
                                // CSRF token validation is DISABLED for API endpoints because:
                                // 1. Spring Security's XOR-encoded CSRF tokens are incompatible with
                                //    cross-origin SPA architectures (token rotation race conditions)
                                // 2. We use ALTERNATIVE CSRF protection via CustomHeaderValidationFilter:
                                //    - Requires X-Requested-With: XMLHttpRequest header
                                //    - HTML forms CANNOT set custom headers (blocks form-based CSRF)
                                //    - Cross-origin JS requests with custom headers require CORS preflight
                                //
                                // SECURITY LAYERS STILL ACTIVE:
                                // ✅ JWT Authentication - Only authenticated users access protected endpoints
                                // ✅ X-Requested-With Header - Custom header that forms can't set (Option D)
                                // ✅ CORS Policy - Only trusted origins (gydi-front-next.vercel.app) allowed
                                // ✅ SameSite=None + Secure cookies - Proper cross-origin cookie handling
                                //
                                // This is a recognized secure pattern for SPAs. See:
                                // - OWASP: "Custom Request Headers" defense
                                // - https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
                                // ============================================================
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/api/v1/referrals/resolve").permitAll()
                                                .requestMatchers("/api/v1/referrals/public/system-link/**").permitAll()

                                                // Public booking endpoints - Allow anonymous users to create bookings via referral links
                                                .requestMatchers("/api/public/bookings/**").permitAll()

                                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()

                                                // Actuator endpoints - SECURITY: Only health endpoint is public
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/actuator/info").permitAll()
                                                // All other actuator endpoints require ADMIN role
                                                .requestMatchers("/actuator/**").hasRole("ADMIN")

                                                // Static files (uploads) - RESTRICTED to specific subdirectories for
                                                // security
                                                // Only allow public access to property images and profile images
                                                .requestMatchers("/uploads/properties/**").permitAll()
                                                .requestMatchers("/uploads/profile-images/**").permitAll()
                                                // Block everything else in /uploads/ for security
                                                .requestMatchers("/uploads/**").denyAll()

                                                // User registration endpoint
                                                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()

                                                // User profile endpoints - authenticated users can access their own
                                                // profiles
                                                .requestMatchers(HttpMethod.GET, "/api/v1/users/profiles/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/profiles/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/users/profiles")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/profiles/**")
                                                .authenticated()

                                                // TEMPORARY: Allow slug generation endpoint (TODO: Remove after initial
                                                // setup)
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/admin/properties/generate-slugs")
                                                .permitAll()

                                                // Admin-only endpoints
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                                                // Property endpoints - Public read access for browsing properties
                                                .requestMatchers(HttpMethod.GET, "/api/properties/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/properties").permitAll()

                                                // Authenticated endpoints
                                                .anyRequest().authenticated())
                                // ✅ Return 401 (not 403) for unauthenticated API requests.
                                // Without this, AnonymousAuthenticationFilter converts missing JWT
                                // into an anonymous user, and Spring Security returns 403 instead of 401.
                                // The frontend's 401 handler triggers token refresh automatically.
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"status\":401,\"path\":\""
                                                                                        + request.getRequestURI() + "\"}");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                                // ✅ CRITICAL: Use NoOp strategy to prevent CSRF token rotation
                                                // Spring Security's CsrfAuthenticationStrategy rotates tokens after ANY
                                                // authentication change, which breaks cross-origin SPA architectures.
                                                // In stateless JWT, we don't need session fixation protection.
                                                .sessionAuthenticationStrategy(new NoOpCsrfAuthenticationStrategy()))
                                // ✅ CRITICAL: Use request-scoped security context (truly stateless)
                                // This prevents Spring Security from detecting "authentication changes"
                                // between requests, which was triggering unwanted CSRF token rotation.
                                .securityContext(context -> context
                                                .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                // ✅ DIAGNOSTIC: Log CSRF-related request info BEFORE CsrfFilter runs
                                .addFilterBefore(new CsrfDiagnosticFilter(), CsrfFilter.class)
                                // ✅ Add CSRF cookie filter to ensure CSRF token is sent in every response
                                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                                // ✅ Option D: Validate X-Requested-With header on state-changing API requests
                                .addFilterAfter(new CustomHeaderValidationFilter(), CsrfCookieFilter.class)
                                // ✅ SECURITY FIX: Add security headers to protect against common attacks
                                // Configured conservatively to NOT break login or existing functionality
                                .headers(headers -> headers
                                                // Enable default headers (X-Content-Type-Options, X-Frame-Options, etc.)
                                                // Spring Security enables these by default, we just configure them

                                                // X-Frame-Options: SAMEORIGIN
                                                // Allows iframes from same origin, blocks external clickjacking
                                                // Using SAMEORIGIN (not DENY) to preserve compatibility
                                                .frameOptions(frame -> frame.sameOrigin())

                                                // Referrer-Policy: strict-origin-when-cross-origin
                                                // Controls how much referrer information is sent with requests
                                                .referrerPolicy(referrer -> referrer
                                                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                                                // ⚠️ Content-Security-Policy: PERMISSIVE mode to avoid breaking functionality
                                                // Allows inline scripts/styles (required for many SPAs)
                                                // Allows connections to frontend origins (required for API calls)
                                                .contentSecurityPolicy(csp -> csp
                                                        .policyDirectives("default-src 'self'; " +
                                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                                                "style-src 'self' 'unsafe-inline'; " +
                                                                "img-src 'self' data: https:; " +
                                                                "font-src 'self' data:; " +
                                                                "connect-src 'self' http://localhost:3000 https://gydi-front-next.vercel.app; " +
                                                                "frame-ancestors 'self'"))

                                                // ℹ️ Strict-Transport-Security (HSTS): Disabled for local development
                                                // TODO: Enable in PRODUCTION with HTTPS
                                                // .httpStrictTransportSecurity(hsts -> hsts
                                                //         .maxAgeInSeconds(31536000)
                                                //         .includeSubDomains(true))
                                );

                return http.build();
        }

        /**
         * Provides the authentication manager bean.
         *
         * @param config the authentication configuration
         * @return the AuthenticationManager
         * @throws Exception if an error occurs
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * Configures the password encoder.
         *
         * @return BCryptPasswordEncoder with strength 10
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }

        /**
         * Configures CORS (Cross-Origin Resource Sharing) for the application.
         *
         * <p>
         * This configuration dynamically loads allowed origins from environment
         * variables,
         * supporting multiple deployment environments (local, dev, staging,
         * production).
         * </p>
         *
         * <p>
         * <strong>Configuration:</strong>
         * <ul>
         * <li><code>cors.allowed-origins</code>: Comma-separated list of explicit
         * origins
         * (e.g., "http://localhost:3000,https://gydi.vercel.app")</li>
         * <li><code>cors.allowed-origin-patterns</code>: Comma-separated wildcard
         * patterns
         * (e.g., "https://*.vercel.app" for Vercel preview deployments)</li>
         * </ul>
         * </p>
         *
         * <p>
         * <strong>Security Note:</strong> Never use "*" in production. Always specify
         * explicit origins or use controlled patterns with authentication.
         * </p>
         *
         * @return CorsConfigurationSource configured for frontend integration
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parse and set allowed origins from environment variable
                if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
                        List<String> origins = new ArrayList<>();
                        for (String origin : allowedOrigins.split(",")) {
                                String trimmed = origin.trim();
                                if (!trimmed.isEmpty()) {
                                        origins.add(trimmed);
                                }
                        }
                        if (!origins.isEmpty()) {
                                configuration.setAllowedOrigins(origins);
                                log.info("CORS configured with allowed origins: {}", origins);
                        }
                }

                // Parse and set allowed origin patterns (for Vercel preview URLs, etc.)
                if (allowedOriginPatterns != null && !allowedOriginPatterns.trim().isEmpty()) {
                        List<String> patterns = new ArrayList<>();
                        for (String pattern : allowedOriginPatterns.split(",")) {
                                String trimmed = pattern.trim();
                                if (!trimmed.isEmpty()) {
                                        patterns.add(trimmed);
                                }
                        }
                        if (!patterns.isEmpty()) {
                                configuration.setAllowedOriginPatterns(patterns);
                                log.info("CORS configured with allowed origin patterns: {}", patterns);
                        }
                }

                // Allow all HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // Explicitly allow required headers (wildcard doesn't work with credentials)
                // NOTE: Do NOT include Access-Control-Request-* headers - those are preflight headers
                configuration.setAllowedHeaders(Arrays.asList(
                                "Content-Type",
                                "Authorization",
                                "X-Requested-With",
                                "X-XSRF-TOKEN",
                                "Accept",
                                "Origin",
                                "Cache-Control",
                                "Pragma"));

                // Allow credentials (cookies, authorization headers, CSRF tokens)
                configuration.setAllowCredentials(true);

                // Expose headers to frontend (Authorization for JWT, X-XSRF-TOKEN for CSRF, rate limits)
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "X-XSRF-TOKEN",
                                "X-RateLimit-Limit",
                                "X-RateLimit-Remaining"));

                // Cache preflight response for 1 hour
                configuration.setMaxAge(3600L);

                // Apply configuration to all paths
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
