package com.affiliate.rentals.gydi.users.infrastructure.in.rest;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;
import com.affiliate.rentals.gydi.shared.infrastructure.storage.StorageException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for handling profile image uploads.
 *
 * <p>Uses StoragePort abstraction for flexibility across environments:</p>
 * <ul>
 *   <li><b>Production</b>: AWS S3 (S3StorageService)</li>
 *   <li><b>Development</b>: Local filesystem (LocalFileSystemStorageAdapter)</li>
 *   <li><b>Testing</b>: In-memory (InMemoryStorageAdapter)</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@RestController
@RequestMapping("/api/v1/users/profiles/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profile Images", description = "Profile image management API")
public class ProfileImageController {

    private final StoragePort storagePort;

    /**
     * Upload a profile cover image to S3.
     *
     * <p>Uploads the image to S3 and returns the public URL.
     * The URL can then be saved to the user profile via the update profile endpoint.</p>
     *
     * @param file the image file to upload
     * @param authentication the authenticated user
     * @return the public S3 URL of the uploaded image
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Upload profile image",
            description = "Upload a profile cover image to AWS S3 and return the public URL. Max file size: 5MB. Allowed formats: JPG, PNG, GIF, WebP.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Image uploaded successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"imageUrl\": \"https://bucket.s3.amazonaws.com/profile-images/uuid.jpg\"}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid file (wrong format, too large, or empty)"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error during upload")
    })
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        log.info("Uploading profile image for user: {}", authentication.getName());

        try {
            String imageUrl = storagePort.uploadFile(file, "profile-images");

            log.info("Profile image uploaded successfully: {}", imageUrl);

            return ResponseEntity.ok(Map.of(
                    "imageUrl", imageUrl,
                    "message", "Image uploaded successfully"
            ));

        } catch (StorageException e) {
            log.error("Failed to upload profile image: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error uploading profile image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal server error during upload"
            ));
        }
    }

    /**
     * Delete a profile image from S3.
     *
     * <p>Deletes an image from S3 using its URL.
     * This is useful when a user wants to remove their profile image or replace it with a new one.</p>
     *
     * @param imageUrl the S3 URL of the image to delete
     * @param authentication the authenticated user
     * @return success message
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete profile image",
            description = "Delete a profile image from AWS S3 using its URL",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid image URL"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> deleteProfileImage(
            @RequestParam("imageUrl") String imageUrl,
            Authentication authentication) {

        log.info("Deleting profile image for user: {}", authentication.getName());

        try {
            storagePort.deleteFileByUrl(imageUrl);

            log.info("Profile image deleted successfully: {}", imageUrl);

            return ResponseEntity.ok(Map.of(
                    "message", "Image deleted successfully"
            ));

        } catch (Exception e) {
            log.error("Failed to delete profile image: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to delete image"
            ));
        }
    }
}
