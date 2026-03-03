package com.affiliate.rentals.gydi.shared.infrastructure.in.sqs;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class SqsEmailListener {

    private final SesV2Client sesV2Client;

    @Value("${app.email.sender:no-reply@gydi.app}")
    private String senderEmail;

    @Value("${app.email.copy-recipient:gydiproperties@gmail.com}")
    private String bccEmail;

    @SqsListener("${app.email.sqs-queue-name:gydi-email-queue-prod}")
    public void processEmailEvent(EmailMessage message) {
        log.info("Processing email from SQS queue to {}", message.toEmail());

        try {
            Destination destination = Destination.builder()
                    .toAddresses(message.toEmail())
                    .bccAddresses(bccEmail) // Blind copy to gydiproperties
                    .build();

            Content subject = Content.builder().data(message.subject()).build();

            Body body = Body.builder()
                    .html(Content.builder().data(message.htmlBody()).build())
                    .build();

            Message sesMessage = Message.builder()
                    .subject(subject)
                    .body(body)
                    .build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(senderEmail)
                    .destination(destination)
                    .content(EmailContent.builder().simple(sesMessage).build())
                    .build();

            SendEmailResponse response = sesV2Client.sendEmail(request);
            log.info("Email successfully sent via SES v2! Message ID: {}", response.messageId());

        } catch (SesV2Exception e) {
            log.error("Amazon SES v2 failed to send email to {}", message.toEmail(), e);
            // Throwing unchecked exception tells SQS that processing failed.
            // SQS will automatically retry based on queue configuration.
            // After max retries, it will be sent to the DLQ (Dead Letter Queue).
            throw new RuntimeException("SES v2 Service error while sending email", e);
        }
    }
}
