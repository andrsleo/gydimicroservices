package com.affiliate.rentals.gydi.properties.application.usecase;

/**
 * Use case for deleting a property video.
 * Removes the video record from database and deletes the file from storage
 * (S3/local).
 */
public interface DeletePropertyVideoUseCase {

    /**
     * Delete a property video.
     *
     * @param propertyId the property ID
     * @param videoId    the video ID to delete
     * @param userId     the user ID (for authorization)
     * @throws IllegalArgumentException if property or video not found
     * @throws SecurityException        if user is not authorized
     */
    void execute(Long propertyId, Long videoId, Long userId);
}
