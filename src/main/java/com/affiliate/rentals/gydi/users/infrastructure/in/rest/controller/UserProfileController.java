package com.affiliate.rentals.gydi.users.infrastructure.in.rest.controller;

import com.affiliate.rentals.gydi.shared.exception.ApiErrorResponses;
import com.affiliate.rentals.gydi.users.application.dto.CreateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UpdateUserProfileRequest;
import com.affiliate.rentals.gydi.users.application.dto.UserProfileResponse;
import com.affiliate.rentals.gydi.users.application.usecase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile management operations.
 *
 * <p>
 * This controller provides RESTful endpoints for CRUD operations on user
 * profiles.
 * It follows REST best practices with appropriate HTTP methods and status
 * codes,
 * utilizing Java 21 features and modern Spring Boot patterns.
 * </p>
 *
 * <p>
 * The controller delegates business logic to use case classes, maintaining
 * separation of concerns and following hexagonal architecture principles.
 * </p>
 *
 * @author GYDI Development Team
 * @see CreateUserProfileUseCase
 * @see GetUserProfileByIdUseCase
 * @see GetUserProfileByUserIdUseCase
 * @see UpdateUserProfileUseCase
 * @see DeleteUserProfileUseCase
 */
@RestController
@RequestMapping("/api/v1/users/profiles")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "User profile management endpoints")
public class UserProfileController {

        private final CreateUserProfileUseCase createUserProfileUseCase;
        private final GetUserProfileByIdUseCase getUserProfileByIdUseCase;
        private final GetUserProfileByUserIdUseCase getUserProfileByUserIdUseCase;
        private final UpdateUserProfileUseCase updateUserProfileUseCase;
        private final DeleteUserProfileUseCase deleteUserProfileUseCase;

        /**
         * Creates a new user profile.
         *
         * @param request the profile creation request
         * @return the created profile with HTTP 201 Created status
         */
        @PostMapping
        @Operation(summary = "Create a new user profile", description = "Creates a new profile for a user. Each user can only have one profile (1:1 relationship).")
        @ApiResponse(responseCode = "201", description = "Profile created successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
        @ApiErrorResponses.Create
        public ResponseEntity<UserProfileResponse> createProfile(
                        @Valid @RequestBody CreateUserProfileRequest request) {
                var response = createUserProfileUseCase.execute(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Retrieves a user profile by profile ID.
         *
         * @param id the profile ID
         * @return the profile with HTTP 200 OK status
         */
        @GetMapping("/{id}")
        @Operation(summary = "Get profile by ID", description = "Retrieves a specific user profile by its unique identifier")
        @ApiResponse(responseCode = "200", description = "Profile found", content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
        @ApiErrorResponses.GetOrDelete
        public ResponseEntity<UserProfileResponse> getProfileById(
                        @Parameter(description = "Profile ID", required = true) @PathVariable Long id) {
                var response = getUserProfileByIdUseCase.execute(id);
                return ResponseEntity.ok(response);
        }

        /**
         * Retrieves a user profile by user ID.
         *
         * @param userId the user ID
         * @return the profile with HTTP 200 OK status
         */
        @GetMapping("/user/{userId}")
        @Operation(summary = "Get profile by user ID", description = "Retrieves a user profile associated with a specific user (1:1 relationship)")
        @ApiResponse(responseCode = "200", description = "Profile found", content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
        @ApiErrorResponses.GetOrDelete
        public ResponseEntity<UserProfileResponse> getProfileByUserId(
                        @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
                var response = getUserProfileByUserIdUseCase.execute(userId);
                return ResponseEntity.ok(response);
        }

        /**
         * Updates an existing user profile.
         *
         * <p>
         * This endpoint supports partial updates (PATCH semantics).
         * Only the fields provided in the request will be updated.
         * </p>
         *
         * @param userId  the user ID
         * @param request the profile update request
         * @return the updated profile with HTTP 200 OK status
         */
        @PatchMapping("/user/{userId}")
        @Operation(summary = "Update user profile", description = "Updates an existing user profile. Supports partial updates - only provided fields are updated.")
        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class)))
        @ApiErrorResponses.Update
        public ResponseEntity<UserProfileResponse> updateProfile(
                        @Parameter(description = "User ID", required = true) @PathVariable Long userId,
                        @Valid @RequestBody UpdateUserProfileRequest request) {
                var response = updateUserProfileUseCase.execute(userId, request);
                return ResponseEntity.ok(response);
        }

        /**
         * Deletes a user profile by user ID.
         *
         * @param userId the user ID
         * @return HTTP 204 No Content status
         */
        @DeleteMapping("/user/{userId}")
        @Operation(summary = "Delete user profile", description = "Deletes a user profile. Note: User profiles are automatically deleted when the user is deleted (CASCADE).")
        @ApiResponse(responseCode = "204", description = "Profile deleted successfully")
        @ApiErrorResponses.GetOrDelete
        public ResponseEntity<Void> deleteProfile(
                        @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
                deleteUserProfileUseCase.execute(userId);
                return ResponseEntity.noContent().build();
        }
}