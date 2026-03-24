package com.affiliate.rentals.gydi.properties.infrastructure.scheduler;

import com.affiliate.rentals.gydi.properties.domain.model.Property;
import com.affiliate.rentals.gydi.properties.domain.model.PropertyStatus;
import com.affiliate.rentals.gydi.properties.domain.ports.out.PropertyRepositoryPort;
import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import com.affiliate.rentals.gydi.shared.infrastructure.out.email.EmailTemplateService;
import com.affiliate.rentals.gydi.users.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler that sends reminder emails to hosts whose properties remain in DRAFT status.
 * <p>
 * Runs every Monday at 09:00 AM UTC. For each DRAFT property, it looks up the host
 * and sends a Thymeleaf-rendered reminder email encouraging them to complete and submit
 * their property for review.
 * <p>
 * Email sending is fire-and-forget: a failure for one property does not affect the others.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftPropertyReminderScheduler {

    private final PropertyRepositoryPort propertyRepository;
    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final EmailTemplateService emailTemplateService;

    /**
     * Sends reminder emails for all properties in DRAFT status.
     * Runs every Monday at 09:00 AM UTC.
     */
    @Scheduled(cron = "0 0 9 * * MON", zone = "UTC")
    public void sendDraftPropertyReminders() {
        log.info("Starting DraftPropertyReminderScheduler — querying DRAFT properties");

        List<Property> draftProperties = propertyRepository.findByStatus(PropertyStatus.DRAFT);

        log.info("Found {} DRAFT properties to process", draftProperties.size());

        int sent = 0;
        int failed = 0;

        for (Property property : draftProperties) {
            try {
                userRepository.findById(property.getHostId()).ifPresent(host -> {
                    EmailMessage email = emailTemplateService.buildDraftPropertyReminderEmail(
                            host.email().address(),
                            new EmailTemplateService.DraftPropertyReminderEmailData(
                                    host.name(),
                                    property.getTitle(),
                                    property.getId().getValue()
                            )
                    );
                    emailService.sendEmail(email);
                    log.debug("Draft reminder sent to host {} for property {}", host.id(), property.getId());
                });
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to send draft reminder for property {} (host {}): {}",
                        property.getId(), property.getHostId(), e.getMessage());
            }
        }

        log.info("DraftPropertyReminderScheduler completed — sent: {}, failed: {}", sent, failed);
    }
}
