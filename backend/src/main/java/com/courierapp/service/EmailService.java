package com.courierapp.service;

import com.courierapp.entity.CompanySettings;
import com.courierapp.repository.CompanySettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender defaultMailSender;
    private final CompanySettingsRepository companySettingsRepository;
    private final String defaultFrom;
    private final String appName;
    private final String frontendUrl;

    public EmailService(JavaMailSender defaultMailSender,
                        CompanySettingsRepository companySettingsRepository,
                        @Value("${spring.mail.username}") String defaultFrom,
                        @Value("${app.name:Courier Booking}") String appName,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.defaultMailSender = defaultMailSender;
        this.companySettingsRepository = companySettingsRepository;
        this.defaultFrom = defaultFrom;
        this.appName = appName;
        this.frontendUrl = frontendUrl;
    }

    /** Synchronous test using currently configured (DB or yml) sender — throws on failure. */
    public void sendTestEmail(String toEmail) {
        doSendTest(toEmail, resolveSender());
    }

    /** Synchronous test with explicit SMTP params — does NOT save to DB. */
    public void sendTestEmailWithConfig(String toEmail, String host, int port,
                                        String username, String password,
                                        String fromName, boolean tls) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        props.put("mail.smtp.starttls.required", String.valueOf(tls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        String from = (fromName != null && !fromName.isBlank() ? fromName : appName) + " <" + username + ">";
        doSendTest(toEmail, new ResolvedSender(sender, from));
    }

    private void doSendTest(String toEmail, ResolvedSender s) {
        try {
            MimeMessage msg = s.sender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(s.from());
            helper.setTo(toEmail);
            helper.setSubject(appName + " - SMTP Test Email");
            helper.setText("<html><body style='font-family:Arial,sans-serif'>"
                    + "<h3>SMTP Test Successful</h3>"
                    + "<p>This is a test email from <strong>" + appName + "</strong>.</p>"
                    + "<p>If you received this, your SMTP configuration is working correctly.</p>"
                    + "</body></html>", true);
            s.sender().send(msg);
            log.info("Test email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Test email failed to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
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
                <p style="font-size:12px;color:#999">If you did not request a password reset, please ignore this email.</p>
                </body></html>
                """.formatted(fullName, appName, resetLink, resetLink);
        send(toEmail, subject, body);
    }

    public void sendReportEmail(String toEmail, String subject, String bodyHtml,
                                byte[] attachment, String attachmentName) {
        ResolvedSender s = resolveSender();
        try {
            MimeMessage msg = s.sender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(s.from());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            helper.addAttachment(attachmentName,
                    new org.springframework.core.io.ByteArrayResource(attachment));
            s.sender().send(msg);
            log.info("Report email sent to {}: {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send report email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void send(String to, String subject, String html) {
        ResolvedSender s = resolveSender();
        try {
            MimeMessage msg = s.sender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(s.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            s.sender().send(msg);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /** Prefer DB-stored SMTP settings; fall back to application.yml config. */
    private ResolvedSender resolveSender() {
        try {
            CompanySettings cs = companySettingsRepository.findAll().stream().findFirst().orElse(null);
            if (cs != null
                    && cs.getSmtpHost() != null && !cs.getSmtpHost().isBlank()
                    && cs.getSmtpUsername() != null && !cs.getSmtpUsername().isBlank()
                    && cs.getSmtpPassword() != null && !cs.getSmtpPassword().isBlank()) {

                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(cs.getSmtpHost());
                sender.setPort(cs.getSmtpPort() != null ? cs.getSmtpPort() : 587);
                sender.setUsername(cs.getSmtpUsername());
                sender.setPassword(cs.getSmtpPassword());
                boolean tls = cs.getSmtpTls() == null || cs.getSmtpTls();
                Properties props = sender.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", String.valueOf(tls));
                props.put("mail.smtp.starttls.required", String.valueOf(tls));
                props.put("mail.smtp.connectiontimeout", "15000");
                props.put("mail.smtp.timeout", "15000");
                props.put("mail.smtp.writetimeout", "15000");
                props.put("mail.smtp.ehlo", "true");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                String fromName = cs.getSmtpFromName() != null ? cs.getSmtpFromName() : appName;
                String from = fromName + " <" + cs.getSmtpUsername() + ">";
                return new ResolvedSender(sender, from);
            }
        } catch (Exception e) {
            log.warn("Could not load DB SMTP config, falling back to application.yml: {}", e.getMessage());
        }
        return new ResolvedSender(defaultMailSender, defaultFrom);
    }

    private record ResolvedSender(JavaMailSender sender, String from) {}
}
