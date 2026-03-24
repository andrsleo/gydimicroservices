package com.affiliate.rentals.gydi.shared.infrastructure.in.sqs;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import sibModel.SendSmtpEmailBcc;

import java.util.List;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class SqsEmailListener {

    private final TransactionalEmailsApi brevoApi;

    @Value("${brevo.from-email:no-reply@gydi.app}")
    private String fromEmail;

    @Value("${app.email.copy-recipient:gydiproperties@gmail.com}")
    private String bccEmail;

    @SqsListener("${app.email.sqs-queue-name:gydi-email-queue-prod}")
    public void processEmailEvent(EmailMessage message) {
        log.info("Processing email from SQS queue to {}", message.toEmail());

        try {
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(new SendSmtpEmailSender().email(fromEmail).name("GYDI"));
            sendSmtpEmail.setTo(List.of(new SendSmtpEmailTo().email(message.toEmail())));
            sendSmtpEmail.setBcc(List.of(new SendSmtpEmailBcc().email(bccEmail)));
            sendSmtpEmail.setSubject(message.subject());
            sendSmtpEmail.setHtmlContent(message.htmlBody());
            if (message.plainTextBody() != null) {
                sendSmtpEmail.setTextContent(message.plainTextBody());
            }

            var response = brevoApi.sendTransacEmail(sendSmtpEmail);
            log.info("Email successfully sent via Brevo! Message ID: {}", response.getMessageId());

        } catch (Exception e) {
            log.error("Brevo failed to send email to {}", message.toEmail(), e);
            // Throwing unchecked exception tells SQS that processing failed.
            // SQS will automatically retry based on queue configuration.
            // After max retries, it will be sent to the DLQ (Dead Letter Queue).
            throw new RuntimeException("Brevo service error while sending email", e);
        }
    }
}
