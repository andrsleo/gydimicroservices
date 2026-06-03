package com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.calendar.application.dto.SeasonRegionDto;
import com.affiliate.rentals.gydi.calendar.domain.port.out.SeasonRegionRepositoryPort;
import com.affiliate.rentals.gydi.calendar.infrastructure.out.persistence.repository.SeasonRegionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeasonRegionRepositoryAdapter implements SeasonRegionRepositoryPort {

    private final SeasonRegionJpaRepository jpaRepository;

    public SeasonRegionRepositoryAdapter(SeasonRegionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SeasonRegionDto> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(e -> new SeasonRegionDto(e.getCode(), e.getName()))
                .toList();
    }
}
