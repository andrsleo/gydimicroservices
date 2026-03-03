package com.affiliate.rentals.gydi.users.domain.service;

import org.springframework.stereotype.Component;

@Component
public class UserEmailTemplateFactory {

    public String buildPasswordResetHtml(String userName, String resetLink) {
        // You can use String.format, or even return Thymeleaf/FreeMarker templates
        // here.
        // I am bringing the HTML you already had in GmailEmailService:
        return String.format(
                """
                        <html>
                        <body style='font-family:sans-serif;background:#f5f5f5;margin:0;padding:20px'>
                            <div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1)'>
                                <div style='background:linear-gradient(135deg,hsl(221.2,83.2%%,53.3%%) 0%%,hsl(221.2,83.2%%,63.3%%) 100%%);padding:40px 20px;text-align:center'>
                                    <h1 style='margin:0;color:#fff;font-size:32px'>GYDI</h1>
                                </div>
                                <div style='padding:40px'>
                                    <h2 style='margin:0 0 20px;color:#1a1a1a;font-size:24px'>Hola %s,</h2>
                                    <p style='margin:0 0 20px;color:#4a4a4a;font-size:16px'>We received a request to reset your password.</p>
                                    <div style='text-align:center;padding:20px 0'>
                                        <a href='%s' style='display:inline-block;background:hsl(221.2,83.2%%,53.3%%);color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:16px;font-weight:600'>
                                            Reset Password
                                        </a>
                                    </div>
                                    <p style='margin:20px 0 10px;color:#6a6a6a;font-size:14px'>Or copy this link: <br/> <a href='%s'>%s</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                userName, resetLink, resetLink, resetLink);
    }

    public String buildPasswordResetConfirmationHtml(String userName) {
        // ... build confirmation HTML
        return "";
    }
}
