package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;

@Slf4j
@Service
@Primary
@Profile("test")
public class MockEmailService implements EmailServicePort {

    @Override
    public void sendEmail(EmailMessage emailMessage) {
        log.info("[MOCK] Email would be sent to: {} with subject: {}", emailMessage.templateName(),
                emailMessage.subject());
    }
}
