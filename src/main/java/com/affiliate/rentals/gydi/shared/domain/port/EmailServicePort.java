package com.affiliate.rentals.gydi.shared.domain.port;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;

public interface EmailServicePort {
    void sendEmail(EmailMessage message);
}
