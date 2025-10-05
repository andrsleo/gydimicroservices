package com.affiliate.rentals.gydi.shared.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Standard error response annotations for API endpoints.
 *
 * <p>This class provides reusable composite annotations that declare standard error responses
 * across all REST controllers. Each annotation automatically includes appropriate HTTP status codes
 * and error response schemas.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @GetMapping("/{id}")
 * @Operation(summary = "Get user by ID")
 * @ApiErrorResponses.NotFound
 * public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
 *     // ...
 * }
 * }</pre>
 *
 * @author GYDI Development Team
 */
public @interface ApiErrorResponses {

    /**
     * Standard error response for operations that might not find a resource.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "404", description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface NotFound {
    }

    /**
     * Standard error response for validation failures.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface BadRequest {
    }

    /**
     * Standard error response for authentication failures.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "401", description = "Authentication failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface Unauthorized {
    }

    /**
     * Standard error response for resource conflicts (e.g., duplicate email).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "409", description = "Resource already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface Conflict {
    }

    /**
     * Combined error responses for create operations (400, 409).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Resource already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @interface Create {
    }

    /**
     * Combined error responses for update operations (400, 404).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @interface Update {
    }

    /**
     * Standard error response for get/delete operations (404).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "404", description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetOrDelete {
    }

    /**
     * Standard error response for authentication operations (401).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "401", description = "Authentication failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @interface Auth {
    }
}