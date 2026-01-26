package com.affiliate.rentals.gydi.users.infrastructure.out.email;

import com.affiliate.rentals.gydi.users.domain.ports.EmailServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
@Profile("dev")
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class AmazonSESEmailService implements EmailServicePort {
        private final SesClient sesClient;
        private final String fromEmail;

        public AmazonSESEmailService(@Value("${aws.ses.region:us-east-1}") String region,
                        @Value("${aws.ses.from-email}") String fromEmail,
                        @Value("${aws.ses.access-key}") String accessKey,
                        @Value("${aws.ses.secret-key}") String secretKey) {
                this.fromEmail = fromEmail;
                this.sesClient = SesClient.builder().region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider
                                                .create(AwsBasicCredentials.create(accessKey, secretKey)))
                                .build();
                log.info("SES initialized: region={}, from={}", region, fromEmail);
        }

        @Override
        public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
                sendEmail(toEmail, "Restablece tu contraseña - GYDI", buildResetHtml(userName, resetLink),
                                "Visita: " + resetLink + " (expira en 1h)");
                log.info("Reset email sent to {}", toEmail);
        }

        @Override
        public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
                sendEmail(toEmail, "Contraseña actualizada - GYDI", buildConfirmHtml(userName),
                                "Tu contraseña ha sido actualizada.");
                log.info("Confirmation sent to {}", toEmail);
        }

        @Override
        public void sendSecurityAlertEmail(String toEmail, String userName, String activity) {
                sendEmail(toEmail, "Alerta de seguridad - GYDI", buildAlertHtml(userName, activity),
                                "Alerta: " + activity);
                log.info("Alert sent to {}", toEmail);
        }

        private void sendEmail(String to, String subject, String html, String text) {
                try {
                        sesClient.sendEmail(SendEmailRequest.builder()
                                        .destination(Destination.builder().toAddresses(to).build())
                                        .message(Message.builder()
                                                        .subject(Content.builder().data(subject).charset("UTF-8")
                                                                        .build())
                                                        .body(Body.builder()
                                                                        .html(Content.builder().data(html)
                                                                                        .charset("UTF-8").build())
                                                                        .text(Content.builder().data(text)
                                                                                        .charset("UTF-8").build())
                                                                        .build())
                                                        .build())
                                        .source(fromEmail).build());
                } catch (SesException e) {
                        log.error("SES error: {}", e.awsErrorDetails().errorMessage());
                        throw new RuntimeException("Email send failed", e);
                }
        }

        private String buildResetHtml(String name, String link) {
                return String.format(
                                "<html><body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                                                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>"
                                                + "<div style='background:linear-gradient(135deg,hsl(221.2,83.2%%,53.3%%) 0%%,hsl(221.2,83.2%%,63.3%%) 100%%);padding:40px 20px;text-align:center'>"
                                                + "<h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>"
                                                + "<div style='padding:40px'><h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Hola %s,</h2>"
                                                + "<p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Recibimos una solicitud para restablecer tu contraseña.</p>"
                                                + "<div style='text-align:center;padding:20px 0'>"
                                                + "<a href='%s' style='display:inline-block;background:hsl(221.2,83.2%%,53.3%%);color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:16px;font-weight:600'>Restablecer contraseña</a></div>"
                                                + "<p style='margin:20px 0 10px;color:#6a6a6a;font-size:14px'>O copia este enlace: %s</p></div>"
                                                + "<div style='padding:20px 40px;background:#fff8e6;border-left:4px solid #ffc107'>"
                                                + "<p style='margin:0;color:#7a6000;font-size:14px'><strong>⚠️ Importante:</strong> Expira en 1 hora.</p></div>"
                                                + "<div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>"
                                                + "<p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div></body></html>",
                                name, link, link);
        }

        private String buildConfirmHtml(String name) {
                return String.format(
                                "<html><body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                                                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>"
                                                + "<div style='background:linear-gradient(135deg,#22c55e 0%%,#16a34a 100%%);padding:40px 20px;text-align:center'>"
                                                + "<h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>"
                                                + "<div style='padding:40px;text-align:center'><div style='font-size:48px;margin-bottom:20px'>✅</div>"
                                                + "<h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Contraseña actualizada</h2>"
                                                + "<p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Hola %s,</p>"
                                                + "<p style='margin:0;color:#4a4a4a;font-size:16px'>Tu contraseña ha sido actualizada correctamente.</p></div>"
                                                + "<div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>"
                                                + "<p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div></body></html>",
                                name);
        }

        private String buildAlertHtml(String name, String activity) {
                return String.format(
                                "<html><body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                                                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>"
                                                + "<div style='background:linear-gradient(135deg,#ef4444 0%%,#dc2626 100%%);padding:40px 20px;text-align:center'>"
                                                + "<h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1></div>"
                                                + "<div style='padding:40px;text-align:center'><div style='font-size:48px;margin-bottom:20px'>⚠️</div>"
                                                + "<h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Alerta de Seguridad</h2>"
                                                + "<p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>Hola %s,</p>"
                                                + "<p style='margin:0;color:#4a4a4a;font-size:16px'>%s</p></div>"
                                                + "<div style='padding:30px 40px;background:#f9f9f9;border-top:1px solid #e0e0e0'>"
                                                + "<p style='margin:0;color:#8a8a8a;font-size:12px'>© 2025 GYDI</p></div></div></body></html>",
                                name, activity);
        }
}
