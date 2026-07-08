package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanySettingsRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(max = 20)  String pincode,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 30)  String phone,
        @Size(max = 150) String email,
        @Size(max = 20)  String gstin,
        // SMTP mail config
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String smtpFromName,
        Boolean smtpTls
) {}
