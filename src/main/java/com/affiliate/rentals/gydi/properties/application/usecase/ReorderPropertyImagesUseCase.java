package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderImagesResponse;

import java.util.UUID;

/**
 * Use case for reordering property images and setting cover image.
 */
public interface ReorderPropertyImagesUseCase {

    /**
     * Reorders images for a property and optionally sets cover image.
     *
     * @param propertyId the property ID
     * @param userId the requesting user ID
     * @param request the reorder request
     * @return response with operation result
     */
    ReorderImagesResponse execute(UUID propertyId, Long userId, ReorderImagesRequest request);
}