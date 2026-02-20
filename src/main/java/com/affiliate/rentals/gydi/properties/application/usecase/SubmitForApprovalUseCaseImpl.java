package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.exception.PropertyCannotBePublishedException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.SubmitForApprovalUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubmitForApprovalUseCaseImpl implements SubmitForApprovalUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;

    public SubmitForApprovalUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            PaymentMethodRepositoryPort paymentMethodRepository) {
        this.propertyRepository = propertyRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public Property submitForApproval(SubmitForApprovalCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to submit this property");
        }

        boolean hasPaymentMethod = paymentMethodRepository.hasActivePaymentMethod(command.requestingUserId());
        if (!hasPaymentMethod) {
            throw new PropertyCannotBePublishedException(
                    "Debes agregar un método de pago antes de enviar tu propiedad a revisión");
        }

        property.submitForApproval();

        return propertyRepository.save(property);
    }
}
