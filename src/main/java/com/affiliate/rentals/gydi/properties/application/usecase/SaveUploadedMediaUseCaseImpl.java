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
 * <p>Saves media URLs after the frontend has uploaded files directly to Cloudinary.
 * After saving images, triggers an auto-transition check: a DRAFT property with
 * enough images and all required fields will automatically move to PENDING_APPROVAL.</p>
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

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            if (!isValidCloudinaryUrl(mediaUrl.url())) {
                throw new IllegalArgumentException("Invalid media URL: " + mediaUrl.url());
            }
        }

        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            property.addImage(mediaUrl.url(), mediaUrl.displayOrder());
        }

        // After adding images, check if the DRAFT property now qualifies for PENDING_APPROVAL
        property.autoTransitionIfReady();

        Property saved = propertyRepository.save(property);

        log.info("Saved {} uploaded images for property {}. Status: {}",
                command.mediaUrls().size(), propertyId, saved.getStatus());

        return saved;
    }

    @Override
    public Property saveUploadedVideos(
            PropertyId propertyId,
            Long userId,
            SaveUploadedMediaCommand command) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(userId)) {
            throw new SecurityException("User is not authorized to modify this property");
        }

        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            if (!isValidCloudinaryUrl(mediaUrl.url())) {
                throw new IllegalArgumentException("Invalid media URL: " + mediaUrl.url());
            }
        }

        for (SaveUploadedMediaCommand.MediaUrl mediaUrl : command.mediaUrls()) {
            String thumbnailUrl = generateVideoThumbnailUrl(mediaUrl.url());
            property.addVideo(mediaUrl.url(), thumbnailUrl, mediaUrl.displayOrder(), mediaUrl.durationSeconds());
        }

        Property saved = propertyRepository.save(property);

        log.info("Saved {} uploaded videos for property {}", command.mediaUrls().size(), propertyId);

        return saved;
    }

    private boolean isValidCloudinaryUrl(String url) {
        return url != null
                && url.startsWith("https://res.cloudinary.com/")
                && (url.contains("/image/upload/") || url.contains("/video/upload/"));
    }

    private String generateVideoThumbnailUrl(String videoUrl) {
        return videoUrl
                .replace("/video/upload/", "/video/upload/so_0/")
                .replaceFirst("\\.(mp4|mov|webm|avi|MOV)$", ".jpg");
    }
}
