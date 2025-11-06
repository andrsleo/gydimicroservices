package com.affiliate.rentals.gydi.properties.domain.ports.out;

import com.affiliate.rentals.gydi.shared.domain.port.StoragePort;

/**
 * Storage port specific to property media (images and videos).
 * Extends the generic StoragePort with property-specific operations.
 */
public interface PropertyStoragePort extends StoragePort {
    
    /**
     * Upload a property image.
     *
     * @param propertyId the property ID
     * @param fileName the file name
     * @param contentType the content type (e.g., image/jpeg)
     * @param data the file data
     * @return the public URL of the uploaded image
     */
    String uploadPropertyImage(String propertyId, String fileName, String contentType, byte[] data);
    
    /**
     * Upload a property video.
     *
     * @param propertyId the property ID
     * @param fileName the file name
     * @param contentType the content type (e.g., video/mp4)
     * @param data the file data
     * @return the public URL of the uploaded video
     */
    String uploadPropertyVideo(String propertyId, String fileName, String contentType, byte[] data);
    
    /**
     * Generate and upload a thumbnail for a video.
     *
     * @param videoUrl the URL of the video
     * @param propertyId the property ID
     * @return the public URL of the generated thumbnail
     */
    String generateAndUploadVideoThumbnail(String videoUrl, String propertyId);
    
    /**
     * Delete a property image by URL.
     *
     * @param imageUrl the image URL
     */
    void deletePropertyImage(String imageUrl);
    
    /**
     * Delete a property video by URL (also deletes associated thumbnail).
     *
     * @param videoUrl the video URL
     * @param thumbnailUrl the thumbnail URL (optional)
     */
    void deletePropertyVideo(String videoUrl, String thumbnailUrl);
}
