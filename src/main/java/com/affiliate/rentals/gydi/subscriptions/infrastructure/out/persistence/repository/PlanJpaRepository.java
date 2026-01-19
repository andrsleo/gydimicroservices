package com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.repository;

import com.affiliate.rentals.gydi.subscriptions.infrastructure.out.persistence.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for PlanEntity persistence operations.
 *
 * <p>This repository provides CRUD operations and custom queries for subscription plans.
 * It extends Spring Data's JpaRepository to leverage built-in persistence methods
 * and adds custom query methods specific to plan management.</p>
 *
 * <p>This is an infrastructure-layer component that will be used by the
 * PlanRepositoryAdapter to implement the PlanRepositoryPort.</p>
 *
 * @author GYDI Development Team
 * @see PlanEntity
 */
@Repository
public interface PlanJpaRepository extends JpaRepository<PlanEntity, Long> {

    /**
     * Finds a plan by its unique code.
     *
     * @param planCode the plan code (e.g., "FREE", "PRO", "ELITE")
     * @return an Optional containing the plan if found, empty otherwise
     */
    Optional<PlanEntity> findByPlanCode(String planCode);

    /**
     * Finds all active plans, ordered by display order.
     *
     * @return a list of active plans
     */
    @Query("SELECT p FROM PlanEntity p WHERE p.isActive = true ORDER BY p.displayOrder ASC, p.monthlyPrice ASC")
    List<PlanEntity> findAllActive();

    /**
     * Finds all featured plans.
     *
     * @return a list of featured plans
     */
    @Query("SELECT p FROM PlanEntity p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.displayOrder ASC")
    List<PlanEntity> findAllFeatured();

    /**
     * Checks if a plan exists with the given code.
     *
     * @param planCode the plan code to check
     * @return true if a plan with this code exists, false otherwise
     */
    boolean existsByPlanCode(String planCode);
}
