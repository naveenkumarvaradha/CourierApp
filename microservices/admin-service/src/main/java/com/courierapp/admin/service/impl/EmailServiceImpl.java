package com.courierapp.admin.service.impl;

import com.courierapp.admin.entity.CompanySettings;
import com.courierapp.admin.repository.CompanySettingsRepository;
import com.courierapp.admin.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final CompanySettingsRepository companySettingsRepository;

    @Override
    public void sendTestEmail(String toEmail) {
        CompanySettings s = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Mail config not found"));
        if (s.getSmtpHost() == null || s.getSmtpHost().isBlank()) {
            throw new RuntimeException("SMTP not configured");
        }
        sendEmail(toEmail, s.getSmtpHost(), s.getSmtpPort() != null ? s.getSmtpPort() : 587,
                s.getSmtpUsername(), s.getSmtpPassword(), s.getSmtpFromName(),
                s.getSmtpTls() != null ? s.getSmtpTls() : true);
    }

    @Override
    public void sendTestEmailWithConfig(String toEmail, String host, int port, String username,
                                        String password, String fromName, boolean tls) {
        sendEmail(toEmail, host, port, username, password, fromName, tls);
    }

    private void sendEmail(String toEmail, String host, int port, String username,
                           String password, String fromName, boolean tls) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (tls) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");

        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            String from = username;
            if (fromName != null && !fromName.isBlank()) {
                from = fromName + " <" + username + ">";
            }
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("ShipDesk - Test Email");
            helper.setText("<p>This is a test email from ShipDesk Admin.</p>", true);
            sender.send(message);
            log.info("Test email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage());
            throw new RuntimeException("SMTP Error: " + e.getMessage(), e);
        }
    }
}
