package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
import com.affiliate.rentals.gydi.users.application.dto.AuthResponse;
import com.affiliate.rentals.gydi.users.application.dto.LoginRequest;
import com.affiliate.rentals.gydi.users.application.usecase.AuthenticateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Constructs a new AuthController with the required use case.
     *
     * @param authenticateUserUseCase the use case for user authentication
     */
    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
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
            description = "Authenticates a user with email and password, returning a JWT token for subsequent requests"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticateUserUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out a user (placeholder for future implementation).
     *
     * <p>In a stateless JWT architecture, logout is typically handled on the client side
     * by removing the token. However, this endpoint can be used to implement token
     * blacklisting or other server-side logout mechanisms in the future.</p>
     *
     * @return HTTP 200 OK status
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Logs out the current user (token invalidation to be implemented)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logout successful"
    )
    public ResponseEntity<Void> logout() {
        // Token invalidation logic will be implemented with security configuration
        // For stateless JWT, this is typically handled client-side by removing the token
        return ResponseEntity.ok().build();
    }

    /**
     * Refreshes an authentication token (placeholder for future implementation).
     *
     * <p>This endpoint can be used to obtain a new JWT token using a refresh token,
     * extending the user's session without requiring re-authentication.</p>
     *
     * @return HTTP 501 Not Implemented status
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh token",
            description = "Obtains a new JWT token using a refresh token (to be implemented)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(
            responseCode = "501",
            description = "Not implemented"
    )
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<AuthResponse> refresh() {
        // Refresh token logic will be implemented with security configuration
        return ResponseEntity.status(501).build();
    }
}
