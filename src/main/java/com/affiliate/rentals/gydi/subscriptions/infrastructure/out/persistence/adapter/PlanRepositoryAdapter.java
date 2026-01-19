package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.adapter;

import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PlanEntity;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.mapper.PlanEntityMapper;
import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository.PlanJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of the PlanRepositoryPort using JPA.
 *
 * <p>This adapter implements the hexagonal architecture's port interface,
 * bridging the domain layer with the infrastructure persistence layer.
 * It uses JPA repositories and mappers to persist and retrieve Plan domain models.</p>
 *
 * @author GYDI Development Team
 * @see PlanRepositoryPort
 * @see PlanJpaRepository
 * @see PlanEntityMapper
 */
@Component
public class PlanRepositoryAdapter implements PlanRepositoryPort {

    private final PlanJpaRepository planJpaRepository;
    private final PlanEntityMapper mapper;

    /**
     * Constructs a new PlanRepositoryAdapter with required dependencies.
     *
     * @param planJpaRepository the JPA repository for plans
     * @param mapper the mapper for Plan-PlanEntity conversion
     */
    public PlanRepositoryAdapter(
            PlanJpaRepository planJpaRepository,
            PlanEntityMapper mapper) {
        this.planJpaRepository = planJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Plan save(Plan plan) {
        PlanEntity entity = mapper.toEntity(plan);
        PlanEntity savedEntity = planJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> findById(Long id) {
        return planJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> findByPlanCode(String planCode) {
        return planJpaRepository.findByPlanCode(planCode)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> findAllActive() {
        return planJpaRepository.findAllActive().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> findAll() {
        return planJpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> findAllFeatured() {
        return planJpaRepository.findAllFeatured().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPlanCode(String planCode) {
        return planJpaRepository.existsByPlanCode(planCode);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        planJpaRepository.deleteById(id);
    }
}
