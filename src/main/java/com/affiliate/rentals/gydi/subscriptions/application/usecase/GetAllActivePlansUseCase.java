package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.PlanResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.model.Plan;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all active subscription plans.
 *
 * <p>This service returns all plans that are currently active and available
 * for subscription. Plans are ordered by display order and price.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class GetAllActivePlansUseCase {

    private final PlanRepositoryPort planRepository;
    private final SubscriptionDtoMapper mapper;

    public GetAllActivePlansUseCase(
            PlanRepositoryPort planRepository,
            SubscriptionDtoMapper mapper) {
        this.planRepository = planRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get all active plans use case.
     *
     * @return list of active plans as DTOs
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> execute() {
        List<Plan> plans = planRepository.findAllActive();

        return plans.stream()
                .map(mapper::toPlanResponse)
                .collect(Collectors.toList());
    }
}
