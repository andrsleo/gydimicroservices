package com.affiliate.rentals.gydi.properties.domain.ports.in;

import com.affiliate.rentals.gydi.properties.application.dto.SaveUploadedMediaCommand;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyId;

/**
 * Use case for saving media URLs after direct Cloudinary upload.
 *
 * <p>This is called after the frontend successfully uploads files directly
 * to Cloudinary. The backend validates the URLs and stores them in the database.</p>
 */
public interface SaveUploadedMediaUseCase {

    /**
     * Save uploaded image URLs to property.
     *
     * @param propertyId the property ID
     * @param userId the requesting user ID
     * @param command the save command with URLs
     * @return the updated property
     */
    Property saveUploadedImages(
        PropertyId propertyId,
        Long userId,
        SaveUploadedMediaCommand command
    );

    /**
     * Save uploaded video URLs to property.
     *
     * @param propertyId the property ID
     * @param userId the requesting user ID
     * @param command the save command with URLs
     * @return the updated property
     */
    Property saveUploadedVideos(
        PropertyId propertyId,
        Long userId,
        SaveUploadedMediaCommand command
    );
}
