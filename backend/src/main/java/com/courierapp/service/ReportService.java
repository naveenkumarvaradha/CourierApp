package com.courierapp.service;

import com.courierapp.dto.report.ReportSummaryResponse;

import java.time.LocalDate;

public interface ReportService {

    /**
     * @param granularity one of: weekly, monthly, yearly, custom
     * @param from        custom start (used when granularity=custom, else derived)
     * @param to          custom end (used when granularity=custom, else today)
     */
    ReportSummaryResponse summary(String granularity, LocalDate from, LocalDate to);

    byte[] exportExcel(String granularity, LocalDate from, LocalDate to);
}
