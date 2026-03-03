package com.affiliate.rentals.gydi.shared.infrastructure.out.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;

import lombok.extern.slf4j.Slf4j;

/**
 * No-op email service adapter that acts as a fallback when no other email
 * service is available.
 *
 * <p>
 * This adapter silently discards all email requests, ensuring the application
 * can start even when email services are not configured.
 * </p>
 *
 * <p>
 * <b>Active when:</b> No other EmailServicePort bean is available AND not in
 * test profile
 * </p>
 *
 * @author GYDI Development Team
 */
@Slf4j
@Service
@Lazy(false)
@Order(Integer.MAX_VALUE)
@Profile("!test")
@ConditionalOnMissingBean(EmailServicePort.class)
public class NoOpEmailServiceAdapter implements EmailServicePort {

    public NoOpEmailServiceAdapter() {
        log.warn(
                "NoOpEmailServiceAdapter initialized - emails will NOT be sent. Configure a real email service for production.");
    }

    @Override
    public void sendEmail(EmailMessage message) {
        log.debug("NoOp: Would send email to {} with subject {}", message.toEmail(), message.subject());
    }
}
