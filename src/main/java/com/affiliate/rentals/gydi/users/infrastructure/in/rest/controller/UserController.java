package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
import com.affiliate.rentals.gydi.shared.security.OwnershipValidator;
import com.affiliate.rentals.gydi.users.application.dto.ChangePasswordRequest;
import com.affiliate.rentals.gydi.users.application.dto.CreateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UpdateUserRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserResponse;
import com.affiliate.rentals.gydi.users.application.usecase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user management operations.
 *
 * <p>This controller provides RESTful endpoints for CRUD operations on users.
 * It follows REST best practices with appropriate HTTP methods and status codes.
 * All endpoints are documented with OpenAPI/Swagger annotations for API documentation.</p>
 *
 * <p>The controller delegates business logic to use case classes, maintaining
 * separation of concerns and following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 * @see CreateUserUseCase
 * @see GetUserByIdUseCase
 * @see GetAllUsersUseCase
 * @see UpdateUserUseCase
 * @see DeleteUserUseCase
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetAllUsersUseCase getAllUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final OwnershipValidator ownershipValidator;

    /**
     * Constructs a new UserController with required use cases.
     *
     * @param createUserUseCase the use case for creating users
     * @param getUserByIdUseCase the use case for retrieving a user by ID
     * @param getAllUsersUseCase the use case for retrieving all users
     * @param updateUserUseCase the use case for updating users
     * @param deleteUserUseCase the use case for deleting users
     * @param changePasswordUseCase the use case for changing password
     * @param ownershipValidator the ownership validator for getting authenticated user ID
     */
    public UserController(
            CreateUserUseCase createUserUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            GetAllUsersUseCase getAllUsersUseCase,
            UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            OwnershipValidator ownershipValidator
    ) {
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.getAllUsersUseCase = getAllUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Creates a new user.
     *
     * @param request the user creation request
     * @return the created user with HTTP 201 Created status
     */
    @PostMapping
    @Operation(summary = "Create a new user", description = "Registers a new user in the system with the provided information")
    @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiErrorResponses.Create
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = createUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user ID
     * @return the user with HTTP 200 OK status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their unique identifier")
    @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiErrorResponses.GetOrDelete
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id
    ) {
        UserResponse response = getUserByIdUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all users.
     *
     * @return a list of all users with HTTP 200 OK status
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all users in the system")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = getAllUsersUseCase.execute();
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing user.
     *
     * @param id the user ID
     * @param request the user update request
     * @return the updated user with HTTP 200 OK status
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user's information")
    @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiErrorResponses.Update
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = updateUserUseCase.execute(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id the user ID
     * @return HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user from the system")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiErrorResponses.GetOrDelete
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id
    ) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the password for the authenticated user.
     *
     * <p>This endpoint allows an authenticated user to change their password.
     * The user must provide their current password for verification before
     * setting a new password.</p>
     *
     * <p><b>SECURITY:</b> This endpoint automatically uses the authenticated user's ID,
     * preventing users from changing other users' passwords.</p>
     *
     * @param request the change password request containing current and new passwords
     * @return HTTP 200 OK status
     */
    @PutMapping("/password")
    @Operation(
            summary = "Change password",
            description = "Changes the authenticated user's password. Requires current password verification."
    )
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiErrorResponses.BadRequest
    @ApiErrorResponses.Unauthorized
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        // SECURITY: Get authenticated user's ID (prevents IDOR)
        Long authenticatedUserId = ownershipValidator.getAuthenticatedUserId();

        changePasswordUseCase.execute(authenticatedUserId, request);
        return ResponseEntity.ok().build();
    }
}
