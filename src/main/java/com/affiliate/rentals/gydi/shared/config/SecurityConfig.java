package com.affiliate.rentals.gydi.shared.config;

import com.affiliate.rentals.gydi.shared.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

        @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
        private String allowedOrigins;

        @Value("${cors.allowed-origin-patterns:}")
        private String allowedOriginPatterns;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
                this.jwtAuthFilter = jwtAuthFilter;
                this.userDetailsService = userDetailsService;
        }

        /**
         * Configures the security filter chain.
         *
         * <p>
         * SECURITY DECISION: CSRF protection is DISABLED for this REST API
         *
         * Justification:
         * 1. Stateless JWT authentication (tokens in Authorization header, not cookies)
         * 2. JWT must be sent explicitly by client - no automatic cookie submission
         * 3. CORS protection restricts allowed origins (defense in depth)
         * 4. SessionCreationPolicy.STATELESS prevents session fixation
         * 5. OWASP recommendation for REST APIs with token-based auth
         *
         * Reference:
         * https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html#csrf
         * "CSRF tokens are not applicable to REST APIs that use proper authentication
         * mechanisms"
         * </p>
         *
         * @param http the HttpSecurity to configure
         * @return the configured SecurityFilterChain
         * @throws Exception if an error occurs during configuration
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF for stateless JWT REST API
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/api/v1/referrals/resolve").permitAll()
                                                .requestMatchers("/api/v1/referrals/public/system-link/**").permitAll()
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
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

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
         * This configuration dynamically loads allowed origins from environment variables,
         * supporting multiple deployment environments (local, dev, staging, production).
         * </p>
         *
         * <p>
         * <strong>Configuration:</strong>
         * <ul>
         * <li><code>cors.allowed-origins</code>: Comma-separated list of explicit origins
         * (e.g., "http://localhost:3000,https://gydi.vercel.app")</li>
         * <li><code>cors.allowed-origin-patterns</code>: Comma-separated wildcard patterns
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

                // Allow all headers
                configuration.setAllowedHeaders(Arrays.asList("*"));

                // Allow credentials (cookies, authorization headers, CSRF tokens)
                configuration.setAllowCredentials(true);

                // Expose headers to frontend (Authorization for JWT, X-XSRF-TOKEN for CSRF)
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "X-XSRF-TOKEN",
                                "XSRF-TOKEN"));

                // Cache preflight response for 1 hour
                configuration.setMaxAge(3600L);

                // Apply configuration to all paths
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
