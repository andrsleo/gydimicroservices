package com.affiliate.rentals.gydi.properties.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.properties.domain.model.PropertyCalendarBlock;
import com.affiliate.rentals.gydi.properties.domain.ports.in.GetPropertyCalendarBlocksUseCase;
import com.affiliate.rentals.gydi.properties.domain.repository.PropertyCalendarBlockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPropertyCalendarBlocksUseCaseImpl implements GetPropertyCalendarBlocksUseCase {

    private final PropertyCalendarBlockRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<PropertyCalendarBlock> getBlocks(Long propertyId) {
        // Fetch blocks ending today or in the future
        return repository.findByPropertyIdAndEndDateAfter(propertyId, LocalDate.now().minusDays(1));
    }
}
