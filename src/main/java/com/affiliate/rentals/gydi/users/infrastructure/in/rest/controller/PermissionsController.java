package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
import com.affiliate.rentals.gydi.users.application.dto.CheckPermissionCommand;
import com.affiliate.rentals.gydi.users.application.dto.CheckResourceLimitCommand;
import com.affiliate.rentals.gydi.users.application.dto.PermissionCheckResponse;
import com.affiliate.rentals.gydi.users.application.dto.ResourceLimitCheckResponse;
import com.affiliate.rentals.gydi.users.application.usecase.CheckPermissionUseCase;
import com.affiliate.rentals.gydi.users.application.usecase.CheckResourceLimitUseCase;
import com.affiliate.rentals.gydi.users.domain.model.Permission;
import com.affiliate.rentals.gydi.users.domain.model.ResourceType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for permission and resource limit evaluation.
 *
 * <p>This controller provides RESTful endpoints for checking user permissions
 * and validating resource limits based on subscription plans. It is used by
 * the frontend and other services to determine if users can perform specific
 * actions or create additional resources.</p>
 *
 * <p>The controller delegates business logic to use case classes, maintaining
 * separation of concerns and following hexagonal architecture principles.</p>
 *
 * @author GYDI Development Team
 * @see CheckPermissionUseCase
 * @see CheckResourceLimitUseCase
 */
@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "Permission and resource limit evaluation endpoints")
public class PermissionsController {

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CheckResourceLimitUseCase checkResourceLimitUseCase;

    /**
     * Constructs a new PermissionsController with required use cases.
     *
     * @param checkPermissionUseCase the use case for checking permissions
     * @param checkResourceLimitUseCase the use case for checking resource limits
     */
    public PermissionsController(
            CheckPermissionUseCase checkPermissionUseCase,
            CheckResourceLimitUseCase checkResourceLimitUseCase) {
        this.checkPermissionUseCase = checkPermissionUseCase;
        this.checkResourceLimitUseCase = checkResourceLimitUseCase;
    }

    /**
     * Checks if a user has a specific permission.
     *
     * <p>This endpoint evaluates if the user can perform a specific action based on:</p>
     * <ul>
     *   <li>User role (ADMIN has all permissions)</li>
     *   <li>Account verification status</li>
     *   <li>User capabilities (canPublish, canRefer, canRent)</li>
     *   <li>Subscription plan level</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @param permission the permission to check
     * @return the permission check result with HTTP 200 OK status
     */
    @GetMapping("/check")
    @Operation(
            summary = "Check user permission",
            description = "Evaluates if a user has a specific permission based on role, verification, capabilities, and subscription plan"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Permission check completed successfully",
            content = @Content(schema = @Schema(implementation = PermissionCheckResponse.class))
    )
    @ApiErrorResponses.GetOrDelete
    public ResponseEntity<PermissionCheckResponse> checkPermission(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,

            @Parameter(description = "Permission to check", required = true)
            @RequestParam Permission permission
    ) {
        CheckPermissionCommand command = new CheckPermissionCommand(userId, permission);
        PermissionCheckResponse response = checkPermissionUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a user can create more resources of a specific type.
     *
     * <p>This endpoint evaluates if the user has reached their subscription plan limit
     * for a specific resource type (properties, referrals). It returns:</p>
     * <ul>
     *   <li>Current resource count</li>
     *   <li>Maximum limit for the plan</li>
     *   <li>Whether more resources can be created</li>
     *   <li>Remaining capacity</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @param resourceType the type of resource to check
     * @return the resource limit check result with HTTP 200 OK status
     */
    @GetMapping("/check-limit")
    @Operation(
            summary = "Check resource limit",
            description = "Evaluates if a user can create more resources based on their subscription plan limits"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resource limit check completed successfully",
            content = @Content(schema = @Schema(implementation = ResourceLimitCheckResponse.class))
    )
    @ApiErrorResponses.GetOrDelete
    public ResponseEntity<ResourceLimitCheckResponse> checkResourceLimit(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,

            @Parameter(description = "Resource type to check", required = true)
            @RequestParam ResourceType resourceType
    ) {
        CheckResourceLimitCommand command = new CheckResourceLimitCommand(userId, resourceType);
        ResourceLimitCheckResponse response = checkResourceLimitUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks multiple permissions at once.
     *
     * <p>This endpoint allows checking multiple permissions in a single request,
     * reducing the number of API calls needed by the frontend.</p>
     *
     * @param command the permission check command (validated)
     * @return the permission check result with HTTP 200 OK status
     */
    @PostMapping("/check")
    @Operation(
            summary = "Check user permission (POST)",
            description = "Evaluates if a user has a specific permission (POST method for complex requests)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Permission check completed successfully",
            content = @Content(schema = @Schema(implementation = PermissionCheckResponse.class))
    )
    @ApiErrorResponses.Update
    public ResponseEntity<PermissionCheckResponse> checkPermissionPost(
            @Valid @RequestBody CheckPermissionCommand command
    ) {
        PermissionCheckResponse response = checkPermissionUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks resource limits (POST method).
     *
     * @param command the resource limit check command (validated)
     * @return the resource limit check result with HTTP 200 OK status
     */
    @PostMapping("/check-limit")
    @Operation(
            summary = "Check resource limit (POST)",
            description = "Evaluates if a user can create more resources (POST method for complex requests)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resource limit check completed successfully",
            content = @Content(schema = @Schema(implementation = ResourceLimitCheckResponse.class))
    )
    @ApiErrorResponses.Update
    public ResponseEntity<ResourceLimitCheckResponse> checkResourceLimitPost(
            @Valid @RequestBody CheckResourceLimitCommand command
    ) {
        ResourceLimitCheckResponse response = checkResourceLimitUseCase.execute(command);
        return ResponseEntity.ok(response);
    }
}