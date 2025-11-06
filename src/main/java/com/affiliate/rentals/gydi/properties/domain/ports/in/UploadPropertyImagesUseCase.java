package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

import java.util.List;

/**
 * Use case for uploading images to a property.
 */
public interface UploadPropertyImagesUseCase {
    
    /**
     * Uploads multiple images to a property.
     *
     * @param command the upload command
     * @return the updated Property with new images
     */
    Property uploadImages(UploadImagesCommand command);
    
    /**
     * Command for uploading images
     */
    record UploadImagesCommand(
        PropertyId propertyId,
        Long requestingUserId,
        List<ImageUpload> images
    ) {}
    
    /**
     * Single image upload data
     */
    record ImageUpload(
        String fileName,
        String contentType,
        long fileSizeBytes,
        byte[] data,
        int displayOrder
    ) {
        public void validate() {
            if (fileSizeBytes > 10 * 1024 * 1024) { // 10MB
                throw new IllegalArgumentException("Image size cannot exceed 10MB");
            }
            if (!isValidImageType(contentType)) {
                throw new IllegalArgumentException("Invalid image type. Allowed: JPG, PNG, WebP, HEIC");
            }
        }

        private boolean isValidImageType(String contentType) {
            return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/heic") ||
                contentType.equals("image/heif")
            );
        }
    }
}
