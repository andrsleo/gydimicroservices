package com.affiliate.rentals.gydi.properties.application.usecase;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.ports.in.AdminApprovePropertyUseCase;
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
public class AdminApprovePropertyUseCaseImpl implements AdminApprovePropertyUseCase {

    private final PropertyRepositoryPort propertyRepository;
    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    public AdminApprovePropertyUseCaseImpl(
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
    public Property approveProperty(ApprovePropertyCommand command) {
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        property.approvePendingProperty();

        Property saved = propertyRepository.save(property);

        // Fire-and-forget: email failure must not roll back the approval
        try {
            userRepository.findById(saved.getHostId()).ifPresent(host ->
                sendApprovedEmail(host, saved)
            );
        } catch (Exception e) {
            log.error("Failed to send property approved email for property {} to host {}: {}",
                    saved.getId(), saved.getHostId(), e.getMessage());
        }

        return saved;
    }

    private void sendApprovedEmail(User host, Property property) {
        EmailMessage email = emailTemplateService.buildPropertyApprovedEmail(
                host.email().address(),
                new EmailTemplateService.PropertyApprovedEmailData(
                        host.name(),
                        property.getTitle(),
                        property.getId().getValue()
                )
        );
        emailService.sendEmail(email);
        log.info("Property approved email sent to host {} for property {}", host.id(), property.getId());
    }
}
