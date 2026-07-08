package com.courierapp.admin.dto.admin;

public record MailConfigRequest(String smtpHost, Integer smtpPort, String smtpUsername, String smtpPassword, String smtpFromName, Boolean smtpTls) {}
