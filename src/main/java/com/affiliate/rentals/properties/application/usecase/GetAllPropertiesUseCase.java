package com.affiliate.rentals.properties.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.affiliate.rentals.properties.application.dto.PropertyDto;

public interface GetAllPropertiesUseCase {
    Page<PropertyDto> execute(Pageable pageable);
}