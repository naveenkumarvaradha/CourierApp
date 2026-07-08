package com.courierapp.admin.dto.admin;

public record MailConfigResponse(String smtpHost, Integer smtpPort, String smtpUsername, String smtpFromName, Boolean smtpTls, boolean configured) {}
