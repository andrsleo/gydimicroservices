package com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.commissions.domain.model.HostCommission;
import com.affiliate.rentals.gydi.commissions.domain.model.HostCommissionStatus;
import com.affiliate.rentals.gydi.commissions.domain.ports.HostCommissionRepositoryPort;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.mapper.HostCommissionEntityMapper;
import com.affiliate.rentals.gydi.commissions.infrastructure.out.persistence.repository.HostCommissionJpaRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class HostCommissionRepositoryAdapter implements HostCommissionRepositoryPort {

    private final HostCommissionJpaRepository jpaRepository;
    private final HostCommissionEntityMapper mapper;

    public HostCommissionRepositoryAdapter(
        HostCommissionJpaRepository jpaRepository,
        HostCommissionEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public HostCommission save(HostCommission commission) {
        var entity = mapper.toEntity(commission);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<HostCommission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<HostCommission> findByBookingId(Long bookingId) {
        return jpaRepository.findByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<HostCommission> findByHostId(Long hostId) {
        return jpaRepository.findByHostId(hostId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<HostCommission> findByHostIdAndStatus(Long hostId, HostCommissionStatus status) {
        return jpaRepository.findByHostIdAndStatus(hostId, status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<HostCommission> findByStatus(HostCommissionStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<HostCommission> findCommissionsForRetry(LocalDateTime now) {
        return jpaRepository.findCommissionsForRetry(now).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByBookingId(Long bookingId) {
        return jpaRepository.existsByBookingId(bookingId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
