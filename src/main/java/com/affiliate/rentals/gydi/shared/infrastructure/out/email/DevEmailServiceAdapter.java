package com.affiliate.rentals.gydi.shared.infrastructure.out.email;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({ "dev", "local" }) // Important: active only locally or dev. Tests use MockEmailService
@RequiredArgsConstructor
public class DevEmailServiceAdapter implements EmailServicePort {

    private final JavaMailSender mailSender;

    @Value("${app.email.override-recipient:gydiproperties@gmail.com}")
    private String overrideRecipient;

    @Value("${spring.mail.username:no-reply@gydi.app}")
    private String fromEmail;

    @Override
    public void sendEmail(EmailMessage message) {
        log.info("DEVELOPMENT MODE: Intercepting email intended for {}, routing to {}", message.toEmail(),
                overrideRecipient);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(overrideRecipient);

            // Prepend original recipient to subject for debugging
            helper.setSubject("[DEV to " + message.toEmail() + "] " + message.subject());
            helper.setText(message.htmlBody(), true);

            mailSender.send(mimeMessage);
            log.info("Dev email successfully sent via SMTP");
        } catch (Exception e) {
            log.error("Failed to send dev email", e);
            throw new RuntimeException("Email failure", e);
        }
    }
}
