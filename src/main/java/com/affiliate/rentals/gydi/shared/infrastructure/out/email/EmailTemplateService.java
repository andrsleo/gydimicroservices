package com.affiliate.rentals.gydi.shared.infrastructure.out.email;

import com.affiliate.rentals.gydi.shared.domain.model.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Infrastructure service for building HTML email messages using Thymeleaf templates.
 * <p>
 * Renders Thymeleaf templates to HTML strings and returns {@link EmailMessage} objects
 * ready for dispatch via {@link com.affiliate.rentals.gydi.shared.domain.port.EmailServicePort}.
 * <p>
 * Each builder method exposes a typed contract via an inner record (*EmailData),
 * guaranteeing type-safety without creating subtypes of EmailMessage.
 * <p>
 * Templates are located under {@code classpath:/templates/email/}.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    // ── Data Records ──────────────────────────────────────────────────────────

    public record PasswordResetEmailData(String userName, String resetLink) {}

    public record PasswordResetConfirmationEmailData(String userName) {}

    public record DraftPropertyReminderEmailData(String hostName, String propertyTitle, Long propertyId) {}

    public record PropertyApprovedEmailData(String hostName, String propertyTitle, Long propertyId) {}

    public record PropertyDeniedEmailData(String hostName, String propertyTitle, String reason) {}

    public record BookingReservedEmailData(String hostName, String guestName, String propertyTitle,
                                           LocalDate checkIn, LocalDate checkOut, BigDecimal totalAmount) {}

    public record BookingFinishedEmailData(String hostName, String guestName, String propertyTitle,
                                           LocalDate checkIn, LocalDate checkOut) {}

    public record HostCommissionChargedEmailData(String hostName, String propertyTitle,
                                                  BigDecimal commissionAmount, BigDecimal bookingAmount,
                                                  String currency) {}

    public record AffiliateCommissionEarnedEmailData(String affiliateName, BigDecimal commissionAmount,
                                                      String currency, String referredGuestName,
                                                      String propertyTitle) {}

    // ── Builder methods ───────────────────────────────────────────────────────

    public EmailMessage buildPasswordResetEmail(String toEmail, PasswordResetEmailData data) {
        String html = render("email/password-reset", ctx -> {
            ctx.setVariable("userName", data.userName());
            ctx.setVariable("resetLink", data.resetLink());
        });
        return new EmailMessage(toEmail, "Restablecer contraseña - GYDI", html, null, null, null);
    }

    public EmailMessage buildPasswordResetConfirmationEmail(String toEmail, PasswordResetConfirmationEmailData data) {
        String html = render("email/password-reset-confirmation", ctx -> {
            ctx.setVariable("userName", data.userName());
        });
        return new EmailMessage(toEmail, "Contraseña actualizada - GYDI", html, null, null, null);
    }

    public EmailMessage buildDraftPropertyReminderEmail(String toEmail, DraftPropertyReminderEmailData data) {
        String html = render("email/draft-property-reminder", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("propertyId", data.propertyId());
        });
        return new EmailMessage(toEmail, "Tu propiedad está esperando ser publicada - GYDI", html, null, null, null);
    }

    public EmailMessage buildPropertyApprovedEmail(String toEmail, PropertyApprovedEmailData data) {
        String html = render("email/property-approved", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("propertyId", data.propertyId());
        });
        return new EmailMessage(toEmail, "¡Tu propiedad fue aprobada! - GYDI", html, null, null, null);
    }

    public EmailMessage buildPropertyDeniedEmail(String toEmail, PropertyDeniedEmailData data) {
        String html = render("email/property-denied", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("reason", data.reason());
        });
        return new EmailMessage(toEmail, "Actualización sobre tu propiedad - GYDI", html, null, null, null);
    }

    public EmailMessage buildBookingReservedEmail(String toEmail, BookingReservedEmailData data) {
        String html = render("email/booking-reserved", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("guestName", data.guestName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("checkIn", data.checkIn());
            ctx.setVariable("checkOut", data.checkOut());
            ctx.setVariable("totalAmount", data.totalAmount());
        });
        return new EmailMessage(toEmail, "Nueva reserva confirmada - GYDI", html, null, null, null);
    }

    public EmailMessage buildBookingFinishedEmail(String toEmail, BookingFinishedEmailData data) {
        String html = render("email/booking-finished", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("guestName", data.guestName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("checkIn", data.checkIn());
            ctx.setVariable("checkOut", data.checkOut());
        });
        return new EmailMessage(toEmail, "Reserva finalizada - GYDI", html, null, null, null);
    }

    public EmailMessage buildHostCommissionChargedEmail(String toEmail, HostCommissionChargedEmailData data) {
        String html = render("email/host-commission-charged", ctx -> {
            ctx.setVariable("hostName", data.hostName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
            ctx.setVariable("commissionAmount", data.commissionAmount());
            ctx.setVariable("bookingAmount", data.bookingAmount());
            ctx.setVariable("currency", data.currency());
        });
        return new EmailMessage(toEmail, "Comisión de plataforma aplicada - GYDI", html, null, null, null);
    }

    public EmailMessage buildAffiliateCommissionEarnedEmail(String toEmail, AffiliateCommissionEarnedEmailData data) {
        String html = render("email/affiliate-commission-earned", ctx -> {
            ctx.setVariable("affiliateName", data.affiliateName());
            ctx.setVariable("commissionAmount", data.commissionAmount());
            ctx.setVariable("currency", data.currency());
            ctx.setVariable("referredGuestName", data.referredGuestName());
            ctx.setVariable("propertyTitle", data.propertyTitle());
        });
        return new EmailMessage(toEmail, "¡Ganaste una comisión! - GYDI", html, null, null, null);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ContextPopulator {
        void populate(Context ctx);
    }

    private String render(String templateName, ContextPopulator populator) {
        Context ctx = new Context();
        populator.populate(ctx);
        return templateEngine.process(templateName, ctx);
    }
}
