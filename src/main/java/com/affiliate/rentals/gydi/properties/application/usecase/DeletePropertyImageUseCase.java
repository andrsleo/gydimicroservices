package com.affiliate.rentals.gydi.properties.application.usecase;

/**
 * Use case for deleting a property image.
 * Removes the image record from database and deletes the file from storage
 * (S3/local).
 */
public interface DeletePropertyImageUseCase {

    /**
     * Delete a property image.
     *
     * @param propertyId the property ID
     * @param imageId    the image ID to delete
     * @param userId     the user ID (for authorization)
     * @throws IllegalArgumentException if property or image not found
     * @throws SecurityException        if user is not authorized
     */
    void execute(Long propertyId, Long imageId, Long userId);
}
