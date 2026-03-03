package com.affiliate.rentals.gydi.shared.infrastructure.out.email;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("prod") // Important: Active ONLY in prod
@RequiredArgsConstructor
public class ProdSqsEmailPublisherAdapter implements EmailServicePort {

    private final SqsTemplate sqsTemplate;

    @Value("${app.email.sqs-queue-name:gydi-email-queue-prod}")
    private String queueName;

    @Override
    public void sendEmail(EmailMessage message) {
        log.info("PROD MODE: Publishing email event to SQS for {}", message.toEmail());

        // Push the EmailMessage object to the queue (SqsTemplate will serialize it to
        // JSON)
        sqsTemplate.send(to -> to.queue(queueName).payload(message));
    }
}
