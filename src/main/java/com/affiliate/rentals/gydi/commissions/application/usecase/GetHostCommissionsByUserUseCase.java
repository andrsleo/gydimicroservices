package com.affiliate.rentals.gydi.commissions.application.usecase;

import com.affiliate.rentals.gydi.commissions.application.dto.HostCommissionDto;
import com.affiliate.rentals.gydi.commissions.application.mapper.HostCommissionMapper;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class GetHostCommissionsByUserUseCase {
    private final HostCommissionRepositoryPort repository;
    private final HostCommissionMapper mapper;

    public GetHostCommissionsByUserUseCase(HostCommissionRepositoryPort repository, HostCommissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<HostCommissionDto> execute(Long hostId) {
        return repository.findByHostId(hostId).stream()
            .map(mapper::toDto)
            .toList();
    }
}
