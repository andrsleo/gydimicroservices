package com.affiliate.rentals.gydi.calendar.application.usecase;

import com.affiliate.rentals.gydi.calendar.domain.exception.SeasonDefinitionNotFoundException;
import com.affiliate.rentals.gydi.calendar.domain.port.in.DeleteSeasonDefinitionUseCase;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonDefinitionRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DeleteSeasonDefinitionUseCaseImpl implements DeleteSeasonDefinitionUseCase {

    private final SeasonDefinitionRepositoryPort repository;

    public DeleteSeasonDefinitionUseCaseImpl(SeasonDefinitionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id) {
        if (!repository.existsById(id)) {
            throw new SeasonDefinitionNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
