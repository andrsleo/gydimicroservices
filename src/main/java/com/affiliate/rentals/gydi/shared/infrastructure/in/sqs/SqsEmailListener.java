package com.affiliate.rentals.gydi.shared.infrastructure.in.sqs;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("prod")
public class SqsEmailListener {

    private final RestClient brevoClient;

    @Value("${brevo.from-email:no-reply@gydi.app}")
    private String fromEmail;

    @Value("${app.email.copy-recipient:gydiproperties@gmail.com}")
    private String bccEmail;

    public SqsEmailListener(@Value("${brevo.api-key}") String apiKey) {
        this.brevoClient = RestClient.builder()
                .baseUrl("https://api.brevo.com")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @SqsListener("${app.email.sqs-queue-name:gydi-email-queue-prod}")
    public void processEmailEvent(EmailMessage message) {
        log.info("Processing email from SQS queue to {}", message.toEmail());

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "GYDI", "email", fromEmail));
            body.put("to", List.of(Map.of("email", message.toEmail())));
            body.put("bcc", List.of(Map.of("email", bccEmail)));
            body.put("subject", message.subject());
            body.put("htmlContent", message.htmlBody());
            if (message.plainTextBody() != null) {
                body.put("textContent", message.plainTextBody());
            }

            var response = brevoClient.post()
                    .uri("/v3/smtp/email")
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            Object messageId = response.getBody() != null ? response.getBody().get("messageId") : "unknown";
            log.info("Email successfully sent via Brevo! MessageId: {}", messageId);

        } catch (Exception e) {
            log.error("Brevo failed to send email to {}", message.toEmail(), e);
            // Throwing unchecked exception tells SQS that processing failed.
            // SQS will retry based on queue configuration, then send to DLQ.
            throw new RuntimeException("Brevo service error while sending email", e);
        }
    }
}
