package com.affiliate.rentals.gydi.users.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.affiliate.rentals.gydi.users.application.dto.CheckPermissionCommand;
import com.affiliate.rentals.gydi.users.application.dto.PermissionCheckResponse;
import com.affiliate.rentals.gydi.users.domain.exception.UserNotFoundException;
import com.affiliate.rentals.gydi.users.domain.model.Permission;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import com.affiliate.rentals.gydi.users.domain.service.PermissionEvaluator;

/**
 * Use case for checking if a user has a specific permission.
 *
 * <p>This service evaluates permissions using the domain's {@link PermissionEvaluator}
 * and returns a detailed response indicating whether the permission is granted
 * and the reason for the decision.</p>
 *
 * <p>The permission evaluation takes into account:</p>
 * <ul>
 *   <li>User role (ADMIN has all permissions)</li>
 *   <li>Account verification status</li>
 *   <li>User capabilities (canPublish, canRefer, canRent)</li>
 *   <li>Subscription plan level</li>
 * </ul>
 *
 * @author GYDI Development Team
 */
@Service
public class CheckPermissionUseCase {

    private final UserRepositoryPort userRepository;

    public CheckPermissionUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Executes the permission check use case.
     *
     * @param command the permission check command
     * @return the permission check response
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public PermissionCheckResponse execute(CheckPermissionCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> UserNotFoundException.withId(command.userId()));

        Permission permission = command.permission();
        boolean granted = PermissionEvaluator.evaluate(user, permission);

        if (granted) {
            return buildGrantedResponse(user, permission);
        } else {
            return buildDeniedResponse(user, permission);
        }
    }

    private PermissionCheckResponse buildGrantedResponse(User user, Permission permission) {
        if (user.isAdmin()) {
            return PermissionCheckResponse.granted(
                    user.id(),
                    permission.name(),
                    "Admin privilege"
            );
        }
        return PermissionCheckResponse.granted(user.id(), permission.name());
    }

    private PermissionCheckResponse buildDeniedResponse(User user, Permission permission) {
        String reason = determineDenialReason(user, permission);
        return PermissionCheckResponse.denied(user.id(), permission.name(), reason);
    }

    private String determineDenialReason(User user, Permission permission) {
        if (permission.requiresVerification() && !user.isAccountVerified()) {
            return "Account not verified";
        }

        return switch (permission) {
            case PROPERTY_PUBLISH -> !user.capabilities().canPublish()
                    ? "Publishing capability disabled"
                    : "Insufficient subscription plan";

            case REFERRAL_GENERATE -> !user.capabilities().canRefer()
                    ? "Referral capability disabled"
                    : "Insufficient subscription plan";           

            case ANALYTICS_VIEW_ADVANCED, ANALYTICS_EXPORT ->
                    "Requires PRO or ELITE subscription plan";

            default -> "Permission not granted";
        };
    }
}