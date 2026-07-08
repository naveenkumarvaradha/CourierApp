package com.courierapp.report.service;

import com.courierapp.report.dto.ReportSummaryResponse;

import java.time.LocalDate;

public interface ReportService {
    ReportSummaryResponse summary(String granularity, LocalDate from, LocalDate to);
    byte[] exportExcel(String granularity, LocalDate from, LocalDate to);
    byte[] exportBookingPdf(LocalDate from, LocalDate to);
    byte[] exportBookingDetailExcel(LocalDate from, LocalDate to, String status);
    byte[] exportBookingDetailPdf(LocalDate from, LocalDate to, String status);
    byte[] exportUserCreationExcel(LocalDate from, LocalDate to);
    byte[] exportUserCreationPdf(LocalDate from, LocalDate to);
    byte[] exportUserInactiveExcel(LocalDate from, LocalDate to);
    byte[] exportUserInactivePdf(LocalDate from, LocalDate to);
    byte[] exportPartyExcel(LocalDate from, LocalDate to);
    byte[] exportPartyPdf(LocalDate from, LocalDate to);
}
