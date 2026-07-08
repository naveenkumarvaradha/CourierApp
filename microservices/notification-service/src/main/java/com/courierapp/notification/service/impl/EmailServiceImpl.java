package com.courierapp.notification.service.impl;

import com.courierapp.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@shipdesk.local}")
    private String fromEmail;

    @Value("${app.mail.from-name:ShipDesk}")
    private String fromName;

    @Override
    public void sendBookingStatusEmail(String toEmail, String recipientName, String bookingNumber,
                                        String status, String remarks) {
        String subject = "Booking " + bookingNumber + " — " + status;
        String body = "<p>Dear " + recipientName + ",</p>"
                + "<p>Your booking <strong>" + bookingNumber + "</strong> status has been updated to: <strong>" + status + "</strong>.</p>"
                + (remarks != null && !remarks.isBlank() ? "<p>Remarks: " + remarks + "</p>" : "")
                + "<p>Thank you for using ShipDesk.</p>";
        sendHtml(toEmail, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        String subject = "ShipDesk — Password Reset Request";
        String body = "<p>Dear " + username + ",</p>"
                + "<p>A password reset was requested for your account.</p>"
                + "<p>Reset token: <strong>" + resetToken + "</strong></p>"
                + "<p>If you did not request this, please ignore this email.</p>";
        sendHtml(toEmail, subject, body);
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String username, String tempPassword) {
        String subject = "Welcome to ShipDesk";
        String body = "<p>Dear " + username + ",</p>"
                + "<p>Your ShipDesk account has been created.</p>"
                + "<p>Username: <strong>" + username + "</strong></p>"
                + "<p>Temporary Password: <strong>" + tempPassword + "</strong></p>"
                + "<p>Please change your password after first login.</p>";
        sendHtml(toEmail, subject, body);
    }

    private void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromName + " <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} | subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
