package com.affiliate.rentals.gydi.collaborations.application.port.out;

import org.springframework.web.multipart.MultipartFile;

/**
 * Output port for delivery asset file storage (Cloudinary).
 */
public interface CollaborationStoragePort {

    /**
     * Uploads a delivery asset file and returns its public URL.
     *
     * @param file        the multipart file to upload
     * @param agreementId used to organize files under a folder path
     * @return the public URL of the uploaded file
     */
    String uploadDeliveryAsset(MultipartFile file, Long agreementId);
}
