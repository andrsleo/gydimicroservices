package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
import com.affiliate.rentals.gydi.shared.security.CookieService;
import com.affiliate.rentals.gydi.shared.security.JwtService;
import com.affiliate.rentals.gydi.shared.security.RateLimitService;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.LoginRequest;
import com.affiliate.rentals.gydi.users.application.dto.RefreshTokenRequest;
import com.affiliate.rentals.gydi.users.application.usecase.AuthenticateUserUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.LogoutUserUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.RefreshTokenUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.RequestPasswordResetUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.ValidateResetTokenUseCase;
import com.affiliate.rentals.gydi.users.domain.exception.InvalidTokenException;
import com.affiliate.rentals.gydi.users.domain.exception.TokenNotFoundException;
import com.affiliate.rentals.gydi.users.domain.exception.RateLimitExceededException;
import com.affiliate.rentals.gydi.users.domain.exception.MissingRefreshTokenException;
import com.affiliate.rentals.gydi.users.application.usecase.ResetPasswordUseCase;
import com.affiliate.rentals.gydi.users.application.dto.ForgotPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.PasswordResetResponse;
import com.affiliate.rentals.gydi.users.application.dto.ResetPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.ValidateTokenResponse;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for authentication operations.
 *
 * <p>
 * This controller provides RESTful endpoints for user authentication,
 * including login functionality. It returns JWT tokens upon successful
 * authentication, which can be used for subsequent authenticated requests.
 * </p>
 *
 * <p>
 * The controller delegates authentication logic to the AuthenticateUserUseCase,
 * maintaining separation of concerns and following hexagonal architecture
 * principles.
 * JWT token generation is handled at the infrastructure layer.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * POST /api/v1/auth/login
 * {
 *   "email": "user@example.com",
 *   "password": "password123"
 * }
 *
 * Response:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tokenType": "Bearer",
 *   "user": {
 *     "id": 1,
 *     "name": "John Doe",
 *     "email": "user@example.com",
 *     "roles": ["GUEST"]
 *   }
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 * @see AuthenticateUserUseCase
 * @see LoginRequest
 * @see AuthResponse
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ValidateResetTokenUseCase validateResetTokenUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;
    private final com.affiliate.rentals.gydi.shared.security.CookieService cookieService;
    private final Environment environment;

    /**
     * Constructs a new AuthController with the required use cases.
     *
     * <p>
     * Using constructor injection following Spring best practices and SOLID
     * principles.
     * </p>
     *
     * @param authenticateUserUseCase     the use case for user authentication
     * @param logoutUserUseCase           the use case for user logout
     * @param refreshTokenUseCase         the use case for token refresh
     * @param requestPasswordResetUseCase the use case for requesting password reset
     * @param validateResetTokenUseCase   the use case for validating reset tokens
     * @param resetPasswordUseCase        the use case for resetting password
     * @param jwtService                  the JWT service for token operations
     * @param rateLimitService            the rate limiting service for preventing
     *                                    abuse
     * @param cookieService               the cookie service for managing
     *                                    authentication cookies
     * @param environment                 the Spring environment for profile detection
     */
    public AuthController(
            AuthenticateUserUseCase authenticateUserUseCase,
            LogoutUserUseCase logoutUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ValidateResetTokenUseCase validateResetTokenUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            JwtService jwtService,
            RateLimitService rateLimitService,
            CookieService cookieService,
            Environment environment) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.validateResetTokenUseCase = validateResetTokenUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
        this.cookieService = cookieService;
        this.environment = environment;
    }

    /**
     * Authenticates a user with email and password.
     *
     * <p>
     * This endpoint validates the user's credentials and returns a JWT token
     * upon successful authentication. The token should be included in the
     * Authorization header of subsequent requests as "Bearer {token}".
     * </p>
     *
     * @param request the login request containing email and password
     * @return an AuthResponse with JWT token and user information
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user with email and password. In production, sets httpOnly cookie. In development, returns token in body.")
    @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    @ApiErrorResponses.TooManyRequests
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        // SECURITY: Rate limiting to prevent brute force attacks
        if (!rateLimitService.tryConsumeAuth(httpRequest)) {
            long remainingAttempts = rateLimitService.getRemainingAuthAttempts(httpRequest);
            long retryAfterSeconds = 900; // 15 minutes in seconds

            // Throw domain exception (handled by GlobalExceptionHandler)
            throw new RateLimitExceededException(remainingAttempts, retryAfterSeconds);
        }

        AuthResponse response = authenticateUserUseCase.execute(request);

        // Add rate limit headers to successful responses too (for transparency)
        long remainingAttempts = rateLimitService.getRemainingAuthAttempts(httpRequest);
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remainingAttempts));
        httpResponse.setHeader("X-RateLimit-Limit", "10");

        // SECURITY: Set httpOnly cookies in BOTH production and development
        // This allows Next.js middleware to verify authentication in both environments
        cookieService.setAuthCookies(httpResponse, response.token(), response.refreshToken());

        if (isProduction()) {
            // Production: Return response WITHOUT tokens in body (security)
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(null) // Don't expose in body
                    .refreshToken(null) // Don't expose in body
                    .tokenType("Bearer")
                    .user(response.user())
                    .build());
        } else {
            // Development: ALSO return tokens in body (for localStorage fallback)
            // AND set cookies (for middleware verification)
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Logs out a user by blacklisting their current access token.
     *
     * <p>
     * This endpoint supports two logout strategies following the Strategy Pattern:
     * </p>
     * <ul>
     * <li><b>Simple Logout:</b> Blacklists only the current access token</li>
     * <li><b>Complete Logout:</b> Blacklists token + revokes all refresh
     * tokens</li>
     * </ul>
     *
     * <p>
     * <b>Dual-Mode Authentication:</b>
     * </p>
     * <ul>
     * <li><b>Production:</b> Extracts token from httpOnly cookie and clears
     * cookies</li>
     * <li><b>Development:</b> Extracts token from Authorization header</li>
     * </ul>
     *
     * <p>
     * Uses Java 21 pattern matching and Optional for clean, type-safe code.
     * </p>
     *
     * @param authHeader       the Authorization header containing the Bearer token
     *                         (development)
     * @param request          the HTTP request for extracting cookies (production)
     * @param response         the HTTP response for clearing cookies (production)
     * @param revokeAllDevices if true, revokes all refresh tokens (logout from all
     *                         devices)
     * @return HTTP 200 OK status
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logs out the current user by blacklisting their access token. " +
            "Use revokeAllDevices=true to logout from all devices. " +
            "In production, clears httpOnly cookies. In development, blacklists token from header.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Logout successful")
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "revokeAllDevices", defaultValue = "false") boolean revokeAllDevices) {

        String token = extractToken(request, authHeader);

        // Validate token is present
        if (token == null || token.isBlank()) {
            throw new TokenNotFoundException();
        }

        // Perform logout (blacklist token)
        if (revokeAllDevices) {
            performCompleteLogout(token);
        } else {
            performSimpleLogout(token);
        }

        // SECURITY: Clear authentication cookies in BOTH production and development
        cookieService.clearAuthCookies(response);

        return ResponseEntity.ok().build();
    }

    /**
     * Performs a simple logout by blacklisting only the current access token.
     *
     * <p>
     * Following Single Responsibility Principle (SOLID).
     * </p>
     *
     * @param token the access token to blacklist
     */
    private void performSimpleLogout(String token) {
        logoutUserUseCase.execute(token);
    }

    /**
     * Performs a complete logout by blacklisting the token and revoking all refresh
     * tokens.
     *
     * <p>
     * Uses Java 21 pattern matching with Optional for null-safe operations.
     * Follows the Fail-Fast principle.
     * </p>
     *
     * @param token the access token to blacklist
     */
    private void performCompleteLogout(String token) {
        // Extract userId directly from JWT token (Java 21 pattern matching)
        Long userId = extractUserIdFromToken(token)
                .orElseThrow(() -> new IllegalStateException(
                        "User ID not found in token. Token may be invalid or from an older version."));

        // Execute complete logout with all refresh tokens revoked
        logoutUserUseCase.execute(token, userId, true);
    }

    /**
     * Extracts the user ID from a JWT token using Java 21 Optional pattern.
     *
     * <p>
     * This method demonstrates defensive programming and null safety.
     * </p>
     *
     * @param token the JWT token
     * @return an Optional containing the user ID if present
     */
    private Optional<Long> extractUserIdFromToken(String token) {
        try {
            return Optional.ofNullable(jwtService.extractUserId(token));
        } catch (Exception e) {
            // Log the exception (in production, use a logger)
            return Optional.empty();
        }
    }

    /**
     * Refreshes an authentication token using a refresh token.
     *
     * <p>
     * This endpoint validates a refresh token and returns a new access token
     * if the refresh token is valid. The refresh token remains valid and can be
     * reused until it expires or is revoked.
     * </p>
     *
     * <p>
     * <b>Dual-Mode Authentication:</b>
     * </p>
     * <ul>
     * <li><b>Production:</b> Reads refresh token from httpOnly cookie, updates
     * cookies</li>
     * <li><b>Development:</b> Reads refresh token from request body, returns tokens
     * in response</li>
     * </ul>
     *
     * @param requestBody  the refresh token request (development mode, optional)
     * @param httpRequest  the HTTP request for extracting cookies (production mode)
     * @param httpResponse the HTTP response for setting cookies (production mode)
     * @return an AuthResponse with a new access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Obtains a new JWT access token using a refresh token. " +
            "In production, reads refresh token from httpOnly cookie. " +
            "In development, reads refresh token from request body.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        // Detect environment (production vs development)
        String refreshToken = null;

        if (isProduction()) {
            // Production: Extract refresh token from cookie only
            refreshToken = cookieService.extractRefreshToken(httpRequest);
        } else {
            // Development: Extract refresh token from request body first
            if (requestBody != null && requestBody.refreshToken() != null) {
                refreshToken = requestBody.refreshToken();
            }

            // Development fallback: Try cookie if not in body
            if (refreshToken == null || refreshToken.isBlank()) {
                refreshToken = cookieService.extractRefreshToken(httpRequest);
            }
        }

        // Validate refresh token is present
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new MissingRefreshTokenException();
        }

        // Execute refresh token use case
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        AuthResponse response = refreshTokenUseCase.execute(request);

        // SECURITY: Set httpOnly cookies in BOTH production and development
        cookieService.setAuthCookies(httpResponse, response.token(), response.refreshToken());

        if (isProduction()) {
            // Production: Return response WITHOUT tokens in body (security)
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(null) // Don't expose in body
                    .refreshToken(null) // Don't expose in body
                    .tokenType("Bearer")
                    .user(response.user())
                    .build());
        } else {
            // Development: ALSO return tokens in body (for localStorage fallback)
            // AND set cookies (for middleware verification)
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Requests a password reset for the given email address.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends a password reset email if the address exists (generic response for security)")
    @ApiResponse(responseCode = "200", description = "Reset email sent (or email doesn't exist)")
    @ApiErrorResponses.BadRequest
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        PasswordResetResponse response = requestPasswordResetUseCase.execute(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates a password reset token.
     */
    @GetMapping("/reset-password/validate/{token}")
    @Operation(summary = "Validate reset token", description = "Checks if a password reset token is valid and not expired")
    @ApiResponse(responseCode = "200", description = "Token validation result")
    public ResponseEntity<ValidateTokenResponse> validateResetToken(@PathVariable String token) {
        ValidateTokenResponse response = validateResetTokenUseCase.execute(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Resets the user's password using a valid token.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user's password using a valid reset token")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiErrorResponses.BadRequest
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetResponse response = resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies user authentication by validating JWT token.
     *
     * <p>
     * This endpoint supports dual-mode authentication following environment
     * detection:
     * </p>
     * <ul>
     * <li><b>Production:</b> Reads access token from httpOnly cookie</li>
     * <li><b>Development:</b> Reads token from Authorization header</li>
     * </ul>
     *
     * <p>
     * Used by frontend middleware to protect routes and verify user session.
     * </p>
     *
     * @param authHeader the Authorization header (development mode)
     * @param request    the HTTP request containing cookies (production mode)
     * @return user information if authenticated
     */
    @GetMapping("/verify")
    @Operation(summary = "Verify authentication", description = "Validates JWT token and returns user information. " +
            "In production, reads token from httpOnly cookie. " +
            "In development, reads token from Authorization header.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Authentication valid", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<AuthResponse> verify(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        // Detect environment (production vs development)
        String token = null;

        token = extractToken(request, authHeader);

        // Validate token is present
        if (token == null || token.isBlank()) {
            throw new TokenNotFoundException();
        }

        // Validate token and extract user information
        try {
            // Validate token is not expired and not blacklisted
            if (!jwtService.validateToken(token)) {
                throw new InvalidTokenException("Token validation failed");
            }

            // Extract user information from token
            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String name = jwtService.extractName(token);
            java.util.List<String> rolesList = jwtService.extractRoles(token);

            // Build user response DTO (record constructor)
            var userResponse = new com.affiliate.rentals.gydi.users.application.dto.UserResponse(
                    userId,
                    email,
                    name,
                    null, // phoneNumber not in JWT
                    rolesList != null ? new java.util.HashSet<>(rolesList) : java.util.Set.of(),
                    null // createdAt not in JWT
            );

            // Return user information (without token in body for security)
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(null) // Don't expose token in response
                    .refreshToken(null)
                    .tokenType("Bearer")
                    .user(userResponse)
                    .build());
        } catch (JwtException e) {
            throw new InvalidTokenException("Token validation failed", e);
        }
    }

    /**
     * Helper method to check if the application is running in production mode.
     *
     * <p>
     * This method uses the Spring Environment bean to detect active profiles.
     * It checks if the profile is "prod" or "production" using the type-safe
     * acceptsProfiles() method, which is the Spring Boot best practice.
     * </p>
     *
     * @return true if running in production mode, false otherwise
     */
    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    /**
     * Extracts JWT token from request using dual-mode strategy.
     * Production: Reads from httpOnly cookie (access_token)
     * Development: Reads from Authorization header (Bearer token) OR cookie (fallback)
     */
    private String extractToken(
            HttpServletRequest request,
            String authHeader) {
        if (isProduction()) {
            // Production: Extract from cookie only
            return cookieService.extractAccessToken(request);
        } else {
            // Development: Try Authorization header first
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            // Development fallback: Try cookie (for middleware verification)
            String cookieToken = cookieService.extractAccessToken(request);
            if (cookieToken != null && !cookieToken.isBlank()) {
                return cookieToken;
            }

            return null;
        }
    }
}
