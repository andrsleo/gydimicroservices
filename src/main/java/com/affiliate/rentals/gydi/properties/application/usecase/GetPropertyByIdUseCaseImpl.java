package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.GetPropertyByIdUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetPropertyByIdUseCaseImpl implements GetPropertyByIdUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public GetPropertyByIdUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public Optional<Property> getProperty(GetPropertyQuery query) {
        return propertyRepository.findById(query.propertyId());
    }
}
