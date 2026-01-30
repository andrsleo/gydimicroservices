package com.affiliate.rentals.gydi.properties.infrastructure.in.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureRequest;
import com.affiliate.rentals.gydi.properties.application.dto.CloudinarySignatureResponse;
import com.affiliate.rentals.gydi.properties.application.dto.PropertyResponse;
import com.affiliate.rentals.gydi.properties.application.dto.SaveUploadedMediaCommand;
import com.affiliate.rentals.gydi.properties.application.mapper.PropertyMapper;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.in.GenerateCloudinarySignaturesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.SaveUploadedMediaUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyImagesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.in.UploadPropertyVideosUseCase;
import com.affiliate.rentals.gydi.shared.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/properties/{propertyId}/media")
@Tag(name = "Property Media", description = "Property media upload endpoints")
public class PropertyMediaController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PropertyMediaController.class);

    private final UploadPropertyImagesUseCase uploadImagesUseCase;
    private final UploadPropertyVideosUseCase uploadVideosUseCase;
    private final GenerateCloudinarySignaturesUseCase generateSignaturesUseCase;
    private final SaveUploadedMediaUseCase saveUploadedMediaUseCase;
    private final PropertyMapper mapper;
    private final JwtService jwtService;

    public PropertyMediaController(
            UploadPropertyImagesUseCase uploadImagesUseCase,
            UploadPropertyVideosUseCase uploadVideosUseCase,
            GenerateCloudinarySignaturesUseCase generateSignaturesUseCase,
            SaveUploadedMediaUseCase saveUploadedMediaUseCase,
            PropertyMapper mapper,
            JwtService jwtService) {
        this.uploadImagesUseCase = uploadImagesUseCase;
        this.uploadVideosUseCase = uploadVideosUseCase;
        this.generateSignaturesUseCase = generateSignaturesUseCase;
        this.saveUploadedMediaUseCase = saveUploadedMediaUseCase;
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @PostMapping("/images")
    public ResponseEntity<PropertyResponse> uploadImages(
            @PathVariable String propertyId,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest httpRequest) throws IOException {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        List<UploadPropertyImagesUseCase.ImageUpload> images = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            images.add(new UploadPropertyImagesUseCase.ImageUpload(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                -1  // Let the domain layer handle displayOrder
            ));
        }

        Property property = uploadImagesUseCase.uploadImages(
            new UploadPropertyImagesUseCase.UploadImagesCommand(
                PropertyId.of(propertyId),
                userId,
                images
            )
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }

    @PostMapping("/videos")
    public ResponseEntity<PropertyResponse> uploadVideos(
            @PathVariable String propertyId,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest httpRequest) throws IOException {

        log.info(">>> uploadVideos CONTROLLER REACHED - propertyId: {}, filesCount: {}",
                propertyId, files != null ? files.length : 0);

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);
        log.info(">>> uploadVideos - userId extracted from JWT: {}", userId);

        List<UploadPropertyVideosUseCase.VideoUpload> videos = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            videos.add(new UploadPropertyVideosUseCase.VideoUpload(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes(),
                -1  // Let the domain layer handle displayOrder
            ));
        }

        Property property = uploadVideosUseCase.uploadVideos(
            new UploadPropertyVideosUseCase.UploadVideosCommand(
                PropertyId.of(propertyId),
                userId,
                videos
            )
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }

    /**
     * NEW FLOW: Generate signed upload URLs for direct Cloudinary uploads.
     *
     * <p><b>Flow:</b></p>
     * <ol>
     *   <li>Frontend requests signed upload URLs (this endpoint)</li>
     *   <li>Backend generates time-limited Cloudinary signatures</li>
     *   <li>Frontend uploads files directly to Cloudinary in parallel</li>
     *   <li>Frontend sends uploaded URLs to backend (see saveUploadedImages/saveUploadedVideos)</li>
     * </ol>
     *
     * <p><b>Benefits:</b></p>
     * <ul>
     *   <li>Parallel uploads (6 concurrent) - faster than sequential backend uploads</li>
     *   <li>No backend timeout - files upload directly to Cloudinary</li>
     *   <li>Per-file progress tracking</li>
     *   <li>Reduced backend load</li>
     *   <li>Supports large files (500MB videos)</li>
     * </ul>
     */
    @PostMapping("/upload-signatures")
    @Operation(summary = "Generate signed upload URLs for direct Cloudinary uploads")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signatures generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<CloudinarySignatureResponse> generateUploadSignatures(
            @PathVariable String propertyId,
            @Valid @RequestBody CloudinarySignatureRequest request,
            HttpServletRequest httpRequest) {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        CloudinarySignatureResponse response = generateSignaturesUseCase.generateSignatures(
                PropertyId.of(propertyId),
                userId,
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * NEW FLOW: Save uploaded image URLs after direct Cloudinary upload.
     *
     * <p>Called after frontend successfully uploads files to Cloudinary.
     * The backend validates and stores the URLs in the database.</p>
     */
    @PostMapping("/images/save-urls")
    @Operation(summary = "Save uploaded image URLs after direct Cloudinary upload")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URLs saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URLs"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<PropertyResponse> saveUploadedImages(
            @PathVariable String propertyId,
            @Valid @RequestBody SaveUploadedMediaCommand command,
            HttpServletRequest httpRequest) {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        Property property = saveUploadedMediaUseCase.saveUploadedImages(
                PropertyId.of(propertyId),
                userId,
                command
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }

    /**
     * NEW FLOW: Save uploaded video URLs after direct Cloudinary upload.
     *
     * <p>Called after frontend successfully uploads files to Cloudinary.
     * The backend validates and stores the URLs in the database.</p>
     */
    @PostMapping("/videos/save-urls")
    @Operation(summary = "Save uploaded video URLs after direct Cloudinary upload")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URLs saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URLs"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "404", description = "Property not found")
    })
    public ResponseEntity<PropertyResponse> saveUploadedVideos(
            @PathVariable String propertyId,
            @Valid @RequestBody SaveUploadedMediaCommand command,
            HttpServletRequest httpRequest) {

        Long userId = jwtService.extractUserIdFromRequest(httpRequest);

        Property property = saveUploadedMediaUseCase.saveUploadedVideos(
                PropertyId.of(propertyId),
                userId,
                command
        );

        return ResponseEntity.ok(mapper.toPropertyResponse(property));
    }
}
