package com.courierapp.admin.dto.admin;

public record CompanySettingsRequest(
        String companyName, String addressLine1, String addressLine2, String city, String state,
        String pincode, String country, String phone, String email, String gstin,
        String smtpHost, Integer smtpPort, String smtpUsername, String smtpPassword,
        String smtpFromName, Boolean smtpTls
) {}
