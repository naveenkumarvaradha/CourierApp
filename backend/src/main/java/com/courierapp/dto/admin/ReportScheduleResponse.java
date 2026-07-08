package com.courierapp.dto.admin;

import java.time.Instant;

public record ReportScheduleResponse(
        Long id,
        String scheduleName,
        String reportType,
        String frequency,
        Integer dayOfWeek,
        Integer dayOfMonth,
        Integer monthOfYear,
        String recipientEmails,
        String fileFormat,
        boolean enabled,
        Instant lastRunAt,
        Instant nextRunAt,
        String createdBy,
        Instant createdAt
) {}
