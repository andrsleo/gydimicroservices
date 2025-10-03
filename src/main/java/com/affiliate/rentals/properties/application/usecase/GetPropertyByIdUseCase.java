package com.affiliate.rentals.properties.application.usecase;

import java.util.UUID;

import com.affiliate.rentals.properties.application.dto.PropertyDto;

public interface GetPropertyByIdUseCase {
    PropertyDto execute(UUID id);
}