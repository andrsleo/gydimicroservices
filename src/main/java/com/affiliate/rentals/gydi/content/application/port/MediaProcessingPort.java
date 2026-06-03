package com.affiliate.rentals.gydi.content.application.port;

/**
 * Output port for external media processing (e.g. Cloudinary transcoding).
 *
 * <p>This interface is a placeholder for future media processing integration.
 * Implementations will handle video transcoding, image optimization, and
 * thumbnail generation.</p>
 *
 * @author GYDI Development Team
 */
public interface MediaProcessingPort {

    /**
     * Initiates async processing of a media file.
     *
     * @param mediaId     the ID of the ContentMedia to process
     * @param originalUrl the original uploaded file URL
     */
    void initiateProcessing(Long mediaId, String originalUrl);
}
