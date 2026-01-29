package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.SaveUploadedMediaCommand;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;
import com.affiliate.rentals.gydi.properties.domain.ports.in.SaveUploadedMediaUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link SaveUploadedMediaUseCase}.
 *
 * <p>Saves media URLs after the frontend has uploaded files directly to Cloudinary.</p>
 */
@Slf4j
@Service
@Transactional
public class SaveUploadedMediaUseCaseImpl implements SaveUploadedMediaUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public SaveUploadedMediaUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Property saveUploadedImages(
            PropertyId propertyId,
            Long userId,
            SaveUploadedMediaCommand command) {

        // 1. Verify property exists and user has permission
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        // 2. Validate URLs are from Cloudinary
        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            if (!isValidCloudinaryUrl(mediaUrl.url())) {
                throw new IllegalArgumentException("Invalid media URL: " + mediaUrl.url());
            }
        }

        // 3. Add images to property
        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            property.addImage(mediaUrl.url(), mediaUrl.displayOrder());
        }

        // 4. Save and return
        Property saved = propertyRepository.save(property);

        log.info("Saved {} uploaded images for property {}", command.mediaUrls().size(), propertyId);

        return saved;
    }

    @Override
    public Property saveUploadedVideos(
            PropertyId propertyId,
            Long userId,
            SaveUploadedMediaCommand command) {

        // 1. Verify property exists and user has permission
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        // 2. Validate URLs are from Cloudinary
        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            if (!isValidCloudinaryUrl(mediaUrl.url())) {
                throw new IllegalArgumentException("Invalid media URL: " + mediaUrl.url());
            }
        }

        // 3. Add videos to property (with auto-generated thumbnails)
        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            // Cloudinary automatically generates thumbnails for videos
            // Format: .../video/upload/v123/folder/filename.mp4
            // Thumbnail: .../video/upload/v123/folder/filename.jpg (same public_id, .jpg extension)
            String thumbnailUrl = generateVideoThumbnailUrl(mediaUrl.url());
            // Duration will be null as we don't know it yet - frontend can update later if needed
            property.addVideo(mediaUrl.url(), thumbnailUrl, mediaUrl.displayOrder(), null);
        }

        // 4. Save and return
        Property saved = propertyRepository.save(property);

        log.info("Saved {} uploaded videos for property {}", command.mediaUrls().size(), propertyId);

        return saved;
    }

    /**
     * Validate that URL is from Cloudinary.
     */
    private boolean isValidCloudinaryUrl(String url) {
        return url != null
                && url.startsWith("https://res.cloudinary.com/")
                && (url.contains("/image/upload/") || url.contains("/video/upload/"));
    }

    /**
     * Generate Cloudinary video thumbnail URL.
     *
     * <p>Cloudinary automatically generates a thumbnail for videos.
     * We just need to change the resource type to 'image' and extension to '.jpg'.</p>
     *
     * <p>Example:</p>
     * <ul>
     *   <li>Video: https://res.cloudinary.com/cloud/video/upload/v123/folder/uuid.mp4</li>
     *   <li>Thumbnail: https://res.cloudinary.com/cloud/video/upload/v123/folder/uuid.jpg</li>
     * </ul>
     */
    private String generateVideoThumbnailUrl(String videoUrl) {
        // Replace extension with .jpg (Cloudinary auto-generates)
        return videoUrl.replaceFirst("\\.(mp4|mov|webm|avi|MOV)$", ".jpg");
    }
}
