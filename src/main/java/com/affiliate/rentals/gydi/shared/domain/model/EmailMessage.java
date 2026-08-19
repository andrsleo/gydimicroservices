package com.affiliate.rentals.gydi.shared.domain.model;

import java.util.Map;

public record EmailMessage(
        String toEmail,
        String subject,
        String htmlBody,
        String plainTextBody,
        // Optional: Useful if you want to use AWS SES Templates later
        String templateName,
        Map<String, String> templateVariables) {
    public EmailMessage {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject cannot be empty");
        }
    }
}
