package com.courierapp.auth.service.impl;

import com.courierapp.auth.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@shipdesk.app}")
    private String from;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String fullName, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
              <h2 style="color:#1d4ed8">Password Reset Request</h2>
              <p>Hi %s,</p>
              <p>Click the button below to reset your ShipDesk password. This link expires in 24 hours.</p>
              <a href="%s" style="display:inline-block;padding:12px 24px;background:#1d4ed8;color:white;
                 text-decoration:none;border-radius:6px;margin:16px 0">Reset Password</a>
              <p style="color:#666;font-size:12px">If you didn't request this, ignore this email.</p>
            </div>
            """.formatted(fullName, link);
        send(to, "ShipDesk — Reset Your Password", html);
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String fullName, String tempPassword) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
              <h2 style="color:#1d4ed8">Welcome to ShipDesk!</h2>
              <p>Hi %s, your account has been created.</p>
              <p>Temporary password: <strong>%s</strong></p>
              <p>Please log in and change your password immediately.</p>
            </div>
            """.formatted(fullName, tempPassword);
        send(to, "Welcome to ShipDesk", html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {} [{}]", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
