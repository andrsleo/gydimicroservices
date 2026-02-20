package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.exception.PropertyCannotBePublishedException;
import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.PublishPropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.subscriptions.domain.ports.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishPropertyUseCaseImpl implements PublishPropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;

    public PublishPropertyUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            PaymentMethodRepositoryPort paymentMethodRepository) {
        this.propertyRepository = propertyRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public Property publishProperty(PublishPropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!property.isOwnedBy(command.requestingUserId())) {
            throw new SecurityException("User is not authorized to publish this property");
        }

        boolean hasPaymentMethod = paymentMethodRepository.hasActivePaymentMethod(command.requestingUserId());
        if (!hasPaymentMethod) {
            throw new PropertyCannotBePublishedException(
                    "Debes agregar un método de pago antes de publicar tu propiedad"
            );
        }

        property.publish();

        return propertyRepository.save(property);
    }
}
