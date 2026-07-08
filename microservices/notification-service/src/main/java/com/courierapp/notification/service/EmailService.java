package com.courierapp.notification.service;

public interface EmailService {
    void sendBookingStatusEmail(String toEmail, String recipientName, String bookingNumber,
                                 String status, String remarks);
    void sendPasswordResetEmail(String toEmail, String username, String resetToken);
    void sendWelcomeEmail(String toEmail, String username, String tempPassword);
}
