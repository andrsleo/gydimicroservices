package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;

@Service
@ConditionalOnMissingBean(EmailServicePort.class)
public class NoOpEmailServiceAdapter implements EmailServicePort {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
        // intentionally empty
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        // intentionally empty
    }

    @Override
    public void sendSecurityAlertEmail(String toEmail, String userName, String activityDescription) {
        // intentionally empty
    }
}
