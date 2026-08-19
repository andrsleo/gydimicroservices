package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.application.dto.ReorderVideosRequest;
import com.affiliate.rentals.gydi.properties.application.dto.ReorderVideosResponse;

public interface ReorderPropertyVideosUseCase {
    ReorderVideosResponse execute(Long propertyId, Long userId, ReorderVideosRequest request);
}
