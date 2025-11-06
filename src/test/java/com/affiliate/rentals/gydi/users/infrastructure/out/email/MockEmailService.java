package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("test")
public class MockEmailService implements EmailServicePort {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
        log.info("[MOCK] Password reset email would be sent to: {} with link: {}", toEmail, resetLink);
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        log.info("[MOCK] Password reset confirmation email would be sent to: {}", toEmail);
    }

    @Override
    public void sendSecurityAlertEmail(String toEmail, String userName, String activityDescription) {
        log.info("[MOCK] Security alert email would be sent to: {} - {}", toEmail, activityDescription);
    }
}
