package com.courierapp.admin.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MailConfigRequest(
        @NotBlank @Size(max = 255) String smtpHost,
        @Min(1) @Max(65535) Integer smtpPort,
        @Size(max = 255) String smtpUsername,
        String smtpPassword,
        @Size(max = 255) String smtpFromName,
        Boolean smtpTls
) {}
