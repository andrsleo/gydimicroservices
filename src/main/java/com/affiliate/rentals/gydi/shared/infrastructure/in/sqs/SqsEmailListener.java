package com.affiliate.rentals.gydi.shared.infrastructure.in.sqs;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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
                .build();
    }

    @SqsListener("${app.email.sqs-queue-name:gydi-email-queue-prod}")
    public void processEmailEvent(EmailMessage message) {
        log.info("Processing email from SQS queue to {}", message.toEmail());

        try {
            String html = message.htmlBody() != null ? message.htmlBody() : "<p>" + message.subject() + "</p>";
            String text = message.plainTextBody();

            BrevoRequest request = new BrevoRequest(
                    new BrevoSender("GYDI", fromEmail),
                    List.of(new BrevoRecipient(message.toEmail())),
                    List.of(new BrevoRecipient(bccEmail)),
                    message.subject(),
                    html,
                    text
            );

            var response = brevoClient.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(BrevoResponse.class);

            String messageId = response.getBody() != null ? response.getBody().messageId() : "unknown";
            log.info("Email successfully sent via Brevo! MessageId: {}", messageId);

        } catch (Exception e) {
            log.error("Brevo failed to send email to {}", message.toEmail(), e);
            // Throwing unchecked exception tells SQS that processing failed.
            // SQS will retry based on queue configuration, then send to DLQ.
            throw new RuntimeException("Brevo service error while sending email", e);
        }
    }

    // ── Brevo API DTOs ──────────────────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BrevoRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            List<BrevoRecipient> bcc,
            String subject,
            String htmlContent,
            String textContent   // null → omitted from JSON (Brevo doesn't require it)
    ) {}

    record BrevoSender(String name, String email) {}

    record BrevoRecipient(String email) {}

    record BrevoResponse(String messageId) {}
}
