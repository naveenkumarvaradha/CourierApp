package com.courierapp.admin.dto.admin;

public record CompanySettingsResponse(
        Long id, String companyName, String addressLine1, String addressLine2, String city, String state,
        String pincode, String country, String phone, String email, String gstin,
        String smtpHost, Integer smtpPort, String smtpUsername, String smtpFromName,
        Boolean smtpTls, boolean smtpConfigured
) {}
