package com.courierapp.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String fullName, String token);
    void sendWelcomeEmail(String to, String fullName, String tempPassword);
}
