package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.application.dto.CheckResourceLimitCommand;
import com.affiliate.rentals.gydi.users.application.dto.ResourceLimitCheckResponse;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.ResourceType;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.CountUserResourcesPort;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PermissionEvaluator;

/**
 * Use case for checking if a user can create more resources of a specific type.
 *
 * <p>This service evaluates resource limits based on the user's subscription plan
 * and current usage. It returns a detailed response indicating:</p>
 * <ul>
 *   <li>Whether the user can create more resources</li>
 *   <li>Current resource count</li>
 *   <li>Maximum limit for the subscription plan</li>
 *   <li>Remaining capacity</li>
 * </ul>
 *
 * <p>Resource limits by plan:</p>
 * <ul>
 *   <li><strong>FREE</strong>: 10 properties, 50 referrals</li>
 *   <li><strong>PRO</strong>: 50 properties, 200 referrals</li>
 *   <li><strong>ELITE</strong>: Unlimited properties and referrals</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class CheckResourceLimitUseCase {

    private final UserRepositoryPort userRepository;
    private final CountUserResourcesPort countResourcesPort;

    public CheckResourceLimitUseCase(
            UserRepositoryPort userRepository,
            CountUserResourcesPort countResourcesPort) {
        this.userRepository = userRepository;
        this.countResourcesPort = countResourcesPort;
    }

    /**
     * Executes the resource limit check use case.
     *
     * @param command the resource limit check command
     * @return the resource limit check response
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public ResourceLimitCheckResponse execute(CheckResourceLimitCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> UserNotFoundException.withId(command.userId()));

        ResourceType resourceType = command.resourceType();
        int currentCount = countResourcesPort.countUserResources(command.userId(), resourceType);

        boolean canExceed = PermissionEvaluator.canExceedLimit(user, resourceType, currentCount);
        int limit = getLimit(user, resourceType);

        if (canExceed) {
            return ResourceLimitCheckResponse.withinLimit(
                    user.id(),
                    resourceType.name(),
                    currentCount,
                    limit,
                    user.activePlan().name()
            );
        } else {
            return ResourceLimitCheckResponse.limitReached(
                    user.id(),
                    resourceType.name(),
                    currentCount,
                    limit,
                    user.activePlan().name()
            );
        }
    }

    private int getLimit(User user, ResourceType resourceType) {
        return switch (resourceType) {
            case PROPERTY -> user.activePlan().getPropertyPublishLimit();
            case REFERRAL -> user.activePlan().getReferralGenerateLimit();
        };
    }
}