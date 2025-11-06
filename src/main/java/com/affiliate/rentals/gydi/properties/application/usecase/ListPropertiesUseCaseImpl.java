package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.ports.in.ListPropertiesUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListPropertiesUseCaseImpl implements ListPropertiesUseCase {

    private final PropertyRepositoryPort propertyRepository;

    public ListPropertiesUseCaseImpl(PropertyRepositoryPort propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public PropertyPage listProperties(ListPropertiesQuery query) {
        PropertyRepositoryPort.PropertySearchSpec spec = new PropertyRepositoryPort.PropertySearchSpec(
            query.status(),
            query.propertyType(),
            query.country(),
            query.city(),
            query.minPrice(),
            query.maxPrice(),
            query.minBedrooms(),
            query.minBathrooms(),
            query.minGuests(),
            query.amenities(),
            query.searchText(),
            query.page(),
            query.size(),
            query.sortBy(),
            query.sortDirection()
        );

        PropertyRepositoryPort.PropertySearchResult result = propertyRepository.findAll(spec);

        return new PropertyPage(
            result.properties(),
            result.totalElements(),
            result.totalPages(),
            result.currentPage(),
            result.pageSize()
        );
    }
}
