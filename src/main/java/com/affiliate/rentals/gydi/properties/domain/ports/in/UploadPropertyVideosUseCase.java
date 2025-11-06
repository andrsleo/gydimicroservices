package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

import java.util.List;

/**
 * Use case for uploading videos to a property.
 */
public interface UploadPropertyVideosUseCase {
    
    /**
     * Uploads multiple videos to a property.
     *
     * @param command the upload command
     * @return the updated Property with new videos
     */
    Property uploadVideos(UploadVideosCommand command);
    
    /**
     * Command for uploading videos
     */
    record UploadVideosCommand(
        PropertyId propertyId,
        Long requestingUserId,
        List<VideoUpload> videos
    ) {}
    
    /**
     * Single video upload data
     */
    record VideoUpload(
        String fileName,
        String contentType,
        long fileSizeBytes,
        byte[] data,
        int displayOrder
    ) {
        public void validate() {
            if (fileSizeBytes > 500 * 1024 * 1024) { // 500MB
                throw new IllegalArgumentException("Video size cannot exceed 500MB");
            }
            if (!isValidVideoType(contentType)) {
                throw new IllegalArgumentException("Invalid video type. Allowed: MP4, WebM, MOV");
            }
        }

        private boolean isValidVideoType(String contentType) {
            return contentType != null && (
                contentType.equals("video/mp4") ||
                contentType.equals("video/webm") ||
                contentType.equals("video/quicktime")
            );
        }
    }
}
