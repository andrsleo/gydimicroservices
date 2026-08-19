package com.affiliate.rentals.gydi.subscriptions.application.usecase;

import com.affiliate.rentals.gydi.subscriptions.application.dto.PaymentMethodResponse;
import com.affiliate.rentals.gydi.subscriptions.application.mapper.SubscriptionDtoMapper;
import com.affiliate.rentals.gydi.subscriptions.domain.model.PaymentMethod;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all payment methods for a user.
 *
 * <p>This service returns all active payment methods associated with a user,
 * ordered with default payment method first.</p>
 *
 * @author GYDI Development Team
 */
@Service
public class GetUserPaymentMethodsUseCase {

    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final SubscriptionDtoMapper mapper;

    public GetUserPaymentMethodsUseCase(
            PaymentMethodRepositoryPort paymentMethodRepository,
            SubscriptionDtoMapper mapper) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
    }

    /**
     * Executes the get user payment methods use case.
     *
     * @param userId the ID of the user
     * @return list of user's active payment methods
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> execute(Long userId) {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findActiveByUserId(userId);

        return paymentMethods.stream()
                .map(mapper::toPaymentMethodResponse)
                .collect(Collectors.toList());
    }
}
