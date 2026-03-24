package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.AdminDenyPropertyUseCase;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.model.User;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AdminDenyPropertyUseCaseImpl implements AdminDenyPropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    public AdminDenyPropertyUseCaseImpl(
            PropertyRepositoryPort propertyRepository,
            UserRepositoryPort userRepository,
            EmailServicePort emailService,
            EmailTemplateService emailTemplateService) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public Property denyProperty(DenyPropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        property.denyProperty(command.reason());

        Property saved = propertyRepository.save(property);

        // Fire-and-forget: email failure must not roll back the denial
        try {
            userRepository.findById(saved.getHostId()).ifPresent(host ->
                sendDeniedEmail(host, saved, command.reason())
            );
        } catch (Exception e) {
            log.error("Failed to send property denied email for property {} to host {}: {}",
                    saved.getId(), saved.getHostId(), e.getMessage());
        }

        return saved;
    }

    private void sendDeniedEmail(User host, Property property, String reason) {
        EmailMessage email = emailTemplateService.buildPropertyDeniedEmail(
                host.email().address(),
                new EmailTemplateService.PropertyDeniedEmailData(
                        host.name(),
                        property.getTitle(),
                        reason
                )
        );
        emailService.sendEmail(email);
        log.info("Property denied email sent to host {} for property {}", host.id(), property.getId());
    }
}
