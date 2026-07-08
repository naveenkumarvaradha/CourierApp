package com.courierapp.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportScheduleRequest(
        @NotBlank String scheduleName,
        @NotBlank String reportType,
        @NotBlank String frequency,
        Integer dayOfWeek,
        Integer dayOfMonth,
        Integer monthOfYear,
        @NotBlank String recipientEmails,
        @NotNull String fileFormat,
        boolean enabled
) {}
