package com.affiliate.rentals.gydi.subscriptions.domain.ports;

import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Plan aggregate.
 *
 * <p>This interface defines the contract for subscription plan persistence operations
 * following the hexagonal architecture pattern.</p>
 *
 * @author GYDI Development Team
 */
public interface PlanRepositoryPort {

    /**
     * Persists a new plan or updates an existing one.
     *
     * @param plan the plan to save
     * @return the saved plan with generated ID if it was new
     */
    Plan save(Plan plan);

    /**
     * Finds a plan by its unique identifier.
     *
     * @param id the plan ID
     * @return an Optional containing the plan if found, empty otherwise
     */
    Optional<Plan> findById(Long id);

    /**
     * Finds a plan by its unique code (e.g., "FREE", "PRO", "ELITE").
     *
     * @param planCode the plan code
     * @return an Optional containing the plan if found, empty otherwise
     */
    Optional<Plan> findByPlanCode(String planCode);

    /**
     * Retrieves all active plans.
     *
     * @return a list of all active plans, ordered by display order
     */
    List<Plan> findAllActive();

    /**
     * Retrieves all plans (active and inactive).
     *
     * @return a list of all plans, ordered by display order
     */
    List<Plan> findAll();

    /**
     * Retrieves all featured plans for promotional display.
     *
     * @return a list of featured plans
     */
    List<Plan> findAllFeatured();

    /**
     * Checks if a plan with the given code exists.
     *
     * @param planCode the plan code to check
     * @return true if a plan with this code exists, false otherwise
     */
    boolean existsByPlanCode(String planCode);

    /**
     * Deletes a plan by its ID.
     *
     * @param id the ID of the plan to delete
     */
    void deleteById(Long id);
}
