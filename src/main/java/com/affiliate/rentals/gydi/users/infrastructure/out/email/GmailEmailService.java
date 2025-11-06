package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("dev")
public class GmailEmailService implements EmailServicePort {
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String devRecipient;

    public GmailEmailService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromEmail,
            @Value("${app.dev-email-recipient:alvargasrod@gmail.com}") String devRecipient) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.devRecipient = devRecipient;
        log.info("Gmail Email Service initialized - DEV MODE - All emails go to: {}", devRecipient);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
        String subject = "[DEV] Restablece tu contraseña - GYDI";
        String html = buildResetHtml(userName, resetLink, toEmail);
        sendEmail(subject, html);
        log.info("Password reset email sent to {} (DEV: actually sent to {})", toEmail, devRecipient);
    }

    @Override
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        String subject = "[DEV] Contraseña actualizada - GYDI";
        String html = buildConfirmHtml(userName, toEmail);
        sendEmail(subject, html);
        log.info("Confirmation email sent to {} (DEV: actually sent to {})", toEmail, devRecipient);
    }

    @Override
    public void sendSecurityAlertEmail(String toEmail, String userName, String activity) {
        String subject = "[DEV] Alerta de seguridad - GYDI";
        String html = buildAlertHtml(userName, activity, toEmail);
        sendEmail(subject, html);
        log.info("Alert email sent to {} (DEV: actually sent to {})", toEmail, devRecipient);
    }

    private void sendEmail(String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(devRecipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.debug("Email sent successfully via Gmail SMTP");
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildResetHtml(String name, String link, String originalTo) {
        return String.format("""
            <html>
            <body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>
            <div style='background:#fff3cd;padding:10px;border-left:4px solid #ffc107;margin-bottom:20px'>
            <strong>🔧 MODO DESARROLLO</strong><br/>
            Email original destinado a: <strong>%s</strong>
            </div>
            <div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>
            <div style='background:linear-gradient(135deg,hsl(221.2,83.2%%,53.3%%) 0%%,hsl(221.2,83.2%%,63.3%%) 100%%);padding:40px 20px;text-align:center'>
            <h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>
            <div style='padding:40px'><h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Hola %s,</h2>
            <p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Recibimos una solicitud para restablecer tu contraseña.</p>
            <div style='text-align:center;padding:20px 0'>
            <a href='%s' style='display:inline-block;background:hsl(221.2,83.2%%,53.3%%);color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:16px;font-weight:600'>Restablecer contraseña</a></div>
            <p style='margin:20px 0 10px;color:#6a6a6a;font-size:14px'>O copia este enlace: %s</p></div>
            <div style='padding:20px 40px;background:#fff8e6;border-left:4px solid #ffc107'>
            <p style='margin:0;color:#7a6000;font-size:14px'><strong>⚠️ Importante:</strong> Expira en 1 hora.</p></div>
            <div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>
            <p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div>
            </body></html>
            """, originalTo, name, link, link);
    }

    private String buildConfirmHtml(String name, String originalTo) {
        return String.format("""
            <html>
            <body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>
            <div style='background:#fff3cd;padding:10px;border-left:4px solid #ffc107;margin-bottom:20px'>
            <strong>🔧 MODO DESARROLLO</strong><br/>
            Email original destinado a: <strong>%s</strong>
            </div>
            <div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>
            <div style='background:linear-gradient(135deg,#22c55e 0%%,#16a34a 100%%);padding:40px 20px;text-align:center'>
            <h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>
            <div style='padding:40px;text-align:center'><div style='font-size:48px;margin-bottom:20px'>✅</div>
            <h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Contraseña actualizada</h2>
            <p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Hola %s,</p>
            <p style='margin:0;color:#4a4a4a;font-size:16px'>Tu contraseña ha sido actualizada correctamente.</p></div>
            <div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>
            <p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div>
            </body></html>
            """, originalTo, name);
    }

    private String buildAlertHtml(String name, String activity, String originalTo) {
        return String.format("""
            <html>
            <body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>
            <div style='background:#fff3cd;padding:10px;border-left:4px solid #ffc107;margin-bottom:20px'>
            <strong>🔧 MODO DESARROLLO</strong><br/>
            Email original destinado a: <strong>%s</strong>
            </div>
            <div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>
            <div style='background:linear-gradient(135deg,#ef4444 0%%,#dc2626 100%%);padding:40px 20px;text-align:center'>
            <h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>
            <div style='padding:40px;text-align:center'><div style='font-size:48px;margin-bottom:20px'>⚠️</div>
            <h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Alerta de Seguridad</h2>
            <p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Hola %s,</p>
            <p style='margin:0;color:#4a4a4a;font-size:16px'>%s</p></div>
            <div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>
            <p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div>
            </body></html>
            """, originalTo, name, activity);
    }
}
