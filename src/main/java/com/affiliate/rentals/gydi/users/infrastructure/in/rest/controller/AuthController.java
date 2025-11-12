package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
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
import com.affiliate.rentals.gydi.users.application.usecase.ResetPasswordUseCase;
import com.affiliate.rentals.gydi.users.application.dto.ForgotPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.PasswordResetResponse;
import com.affiliate.rentals.gydi.users.application.dto.ResetPasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.ValidateTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for authentication operations.
 *
 * <p>This controller provides RESTful endpoints for user authentication,
 * including login functionality. It returns JWT tokens upon successful
 * authentication, which can be used for subsequent authenticated requests.</p>
 *
 * <p>The controller delegates authentication logic to the AuthenticateUserUseCase,
 * maintaining separation of concerns and following hexagonal architecture principles.
 * JWT token generation is handled at the infrastructure layer.</p>
 *
 * <p>Example usage:</p>
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

    /**
     * Constructs a new AuthController with the required use cases.
     *
     * <p>Using constructor injection following Spring best practices and SOLID principles.</p>
     *
     * @param authenticateUserUseCase the use case for user authentication
     * @param logoutUserUseCase the use case for user logout
     * @param refreshTokenUseCase the use case for token refresh
     * @param requestPasswordResetUseCase the use case for requesting password reset
     * @param validateResetTokenUseCase the use case for validating reset tokens
     * @param resetPasswordUseCase the use case for resetting password
     * @param jwtService the JWT service for token operations
     * @param rateLimitService the rate limiting service for preventing abuse
     */
    public AuthController(
            AuthenticateUserUseCase authenticateUserUseCase,
            LogoutUserUseCase logoutUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ValidateResetTokenUseCase validateResetTokenUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            JwtService jwtService,
            RateLimitService rateLimitService
    ) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.validateResetTokenUseCase = validateResetTokenUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Authenticates a user with email and password.
     *
     * <p>This endpoint validates the user's credentials and returns a JWT token
     * upon successful authentication. The token should be included in the
     * Authorization header of subsequent requests as "Bearer {token}".</p>
     *
     * @param request the login request containing email and password
     * @return an AuthResponse with JWT token and user information
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticates a user with email and password. In production, sets httpOnly cookie. In development, returns token in body."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    @ApiErrorResponses.TooManyRequests
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse
    ) {
        // SECURITY: Rate limiting to prevent brute force attacks
        if (!rateLimitService.tryConsumeAuth(httpRequest)) {
            long remainingAttempts = rateLimitService.getRemainingAuthAttempts(httpRequest);
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remainingAttempts));
            httpResponse.setHeader("X-RateLimit-Retry-After", "900"); // 15 minutes in seconds
            return ResponseEntity.status(429)
                    .body(AuthResponse.builder()
                            .token(null)
                            .refreshToken(null)
                            .tokenType(null)
                            .user(null)
                            .build());
        }

        AuthResponse response = authenticateUserUseCase.execute(request);

        // SECURITY: In production, set httpOnly cookie (XSS-proof)
        // In development, return token in body (cross-origin workaround for localhost)
        String environment = System.getProperty("spring.profiles.active", "dev");
        boolean isProduction = "prod".equals(environment) || "production".equals(environment);

        if (isProduction) {
            // Production: Set httpOnly cookies
            setAuthCookies(httpResponse, response.token(), response.refreshToken());

            // Return response WITHOUT tokens in body (security)
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(null) // Don't expose in body
                    .refreshToken(null) // Don't expose in body
                    .tokenType("Bearer")
                    .user(response.user())
                    .build());
        } else {
            // Development: Return tokens in body (for Authorization header)
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Sets authentication cookies (httpOnly for security).
     *
     * @param response the HTTP response
     * @param accessToken the JWT access token
     * @param refreshToken the refresh token
     */
    private void setAuthCookies(
            jakarta.servlet.http.HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        // Access token cookie
        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true); // HTTPS only in production
        accessCookie.setPath("/");
        accessCookie.setMaxAge(24 * 60 * 60); // 24 hours
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        // Refresh token cookie
        if (refreshToken != null) {
            jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true); // HTTPS only in production
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);
        }
    }

    /**
     * Logs out a user by blacklisting their current access token.
     *
     * <p>This endpoint supports two logout strategies following the Strategy Pattern:</p>
     * <ul>
     *   <li><b>Simple Logout:</b> Blacklists only the current access token</li>
     *   <li><b>Complete Logout:</b> Blacklists token + revokes all refresh tokens</li>
     * </ul>
     *
     * <p>Uses Java 21 pattern matching and Optional for clean, type-safe code.</p>
     *
     * @param authHeader the Authorization header containing the Bearer token
     * @param revokeAllDevices if true, revokes all refresh tokens (logout from all devices)
     * @return HTTP 200 OK status
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Logs out the current user by blacklisting their access token. " +
                         "Use revokeAllDevices=true to logout from all devices.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
    )
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "revokeAllDevices", defaultValue = "false") boolean revokeAllDevices
    ) {
        // Extract token from Bearer header
        String token = authHeader.substring(7);

        // Use ternary operator for clean, functional-style conditional logic (Java 21 best practice)
        if (revokeAllDevices) {
            performCompleteLogout(token);
        } else {
            performSimpleLogout(token);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Performs a simple logout by blacklisting only the current access token.
     *
     * <p>Following Single Responsibility Principle (SOLID).</p>
     *
     * @param token the access token to blacklist
     */
    private void performSimpleLogout(String token) {
        logoutUserUseCase.execute(token);
    }

    /**
     * Performs a complete logout by blacklisting the token and revoking all refresh tokens.
     *
     * <p>Uses Java 21 pattern matching with Optional for null-safe operations.
     * Follows the Fail-Fast principle.</p>
     *
     * @param token the access token to blacklist
     */
    private void performCompleteLogout(String token) {
        // Extract userId directly from JWT token (Java 21 pattern matching)
        Long userId = extractUserIdFromToken(token)
                .orElseThrow(() -> new IllegalStateException(
                        "User ID not found in token. Token may be invalid or from an older version."
                ));

        // Execute complete logout with all refresh tokens revoked
        logoutUserUseCase.execute(token, userId, true);
    }

    /**
     * Extracts the user ID from a JWT token using Java 21 Optional pattern.
     *
     * <p>This method demonstrates defensive programming and null safety.</p>
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
     * <p>This endpoint validates a refresh token and returns a new access token
     * if the refresh token is valid. The refresh token remains valid and can be
     * reused until it expires or is revoked.</p>
     *
     * @param request the refresh token request
     * @return an AuthResponse with a new access token
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh token",
            description = "Obtains a new JWT access token using a refresh token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Requests a password reset for the given email address.
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email if the address exists (generic response for security)"
    )
    @ApiResponse(responseCode = "200", description = "Reset email sent (or email doesn't exist)")
    @ApiErrorResponses.BadRequest
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        PasswordResetResponse response = requestPasswordResetUseCase.execute(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates a password reset token.
     */
    @GetMapping("/reset-password/validate/{token}")
    @Operation(
            summary = "Validate reset token",
            description = "Checks if a password reset token is valid and not expired"
    )
    @ApiResponse(responseCode = "200", description = "Token validation result")
    public ResponseEntity<ValidateTokenResponse> validateResetToken(@PathVariable String token) {
        ValidateTokenResponse response = validateResetTokenUseCase.execute(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Resets the user's password using a valid token.
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using a valid reset token"
    )
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiErrorResponses.BadRequest
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetResponse response = resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}

