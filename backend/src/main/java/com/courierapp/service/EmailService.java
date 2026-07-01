package com.courierapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String appName;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        @Value("${app.name:Courier Booking}") String appName,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.appName = appName;
        this.frontendUrl = frontendUrl;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = appName + " - Password Reset Request";
        String body = """
                <html><body style="font-family:Arial,sans-serif;color:#333">
                <h2>Password Reset Request</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>We received a request to reset your password for your <strong>%s</strong> account.</p>
                <p>Click the button below to reset your password. This link is valid for <strong>24 hours</strong>.</p>
                <p style="margin:24px 0">
                  <a href="%s" style="background:#1976d2;color:#fff;padding:12px 24px;text-decoration:none;border-radius:4px;display:inline-block">
                    Reset Password
                  </a>
                </p>
                <p>Or copy and paste this link into your browser:</p>
                <p style="word-break:break-all;color:#1976d2">%s</p>
                <hr/>
                <p style="font-size:12px;color:#999">If you did not request a password reset, please ignore this email. Your password will not change.</p>
                </body></html>
                """.formatted(fullName, appName, resetLink, resetLink);
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(msg);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
