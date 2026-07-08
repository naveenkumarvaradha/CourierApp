package com.courierapp.admin.service;

public interface EmailService {
    void sendTestEmail(String toEmail);
    void sendTestEmailWithConfig(String toEmail, String host, int port, String username,
                                  String password, String fromName, boolean tls);
}
