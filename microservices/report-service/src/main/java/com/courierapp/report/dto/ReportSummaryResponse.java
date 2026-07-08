package com.courierapp.report.dto;

import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(
        String granularity,
        String from,
        String to,
        long totalBookings,
        Map<String, Long> bookingsByStatus,
        List<PeriodStat> periodStats
) {
    public record PeriodStat(String period, long count, Map<String, Long> byStatus) {}
}
