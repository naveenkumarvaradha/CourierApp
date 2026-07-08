package com.courierapp.controller;

import com.courierapp.dto.report.ReportSummaryResponse;
import com.courierapp.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports")
public class ReportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF  = "application/pdf";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ─── Booking summary ──────────────────────────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Booking summary report (JSON)")
    public ReportSummaryResponse summary(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(granularity, from, to);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Export booking summary as Excel")
    public ResponseEntity<byte[]> exportSummaryExcel(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] data = reportService.exportExcel(granularity, from, to);
        return download(data, XLSX, "booking-summary-" + granularity + ".xlsx");
    }

    @GetMapping("/bookings/pdf")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Export booking summary as PDF")
    public ResponseEntity<byte[]> exportBookingPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportBookingPdf(from, to), PDF, "bookings-" + from + "-" + to + ".pdf");
    }

    // ─── Booking detail report ────────────────────────────────────────────────

    @GetMapping("/bookings/detail/excel")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Export booking detail report as Excel (with audit log sheet)")
    public ResponseEntity<byte[]> exportBookingDetailExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return download(reportService.exportBookingDetailExcel(from, to, status), XLSX,
                "booking-detail-" + from + "-" + to + ".xlsx");
    }

    @GetMapping("/bookings/detail/pdf")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Export booking detail report as PDF (with audit log)")
    public ResponseEntity<byte[]> exportBookingDetailPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return download(reportService.exportBookingDetailPdf(from, to, status), PDF,
                "booking-detail-" + from + "-" + to + ".pdf");
    }

    // ─── User reports ─────────────────────────────────────────────────────────

    @GetMapping("/users/creation/excel")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "User creation report as Excel")
    public ResponseEntity<byte[]> exportUserCreationExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportUserCreationExcel(from, to), XLSX,
                "user-creation-" + from + "-" + to + ".xlsx");
    }

    @GetMapping("/users/creation/pdf")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "User creation report as PDF")
    public ResponseEntity<byte[]> exportUserCreationPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportUserCreationPdf(from, to), PDF,
                "user-creation-" + from + "-" + to + ".pdf");
    }

    @GetMapping("/users/inactive/excel")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "User inactive/disabled report as Excel")
    public ResponseEntity<byte[]> exportUserInactiveExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportUserInactiveExcel(from, to), XLSX,
                "user-inactive-" + from + "-" + to + ".xlsx");
    }

    @GetMapping("/users/inactive/pdf")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "User inactive/disabled report as PDF")
    public ResponseEntity<byte[]> exportUserInactivePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportUserInactivePdf(from, to), PDF,
                "user-inactive-" + from + "-" + to + ".pdf");
    }

    // ─── Master (Party) reports ───────────────────────────────────────────────

    @GetMapping("/parties/excel")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Master party report as Excel (with audit log sheet)")
    public ResponseEntity<byte[]> exportPartyExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportPartyExcel(from, to), XLSX,
                "party-report-" + from + "-" + to + ".xlsx");
    }

    @GetMapping("/parties/pdf")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Master party report as PDF (with audit log)")
    public ResponseEntity<byte[]> exportPartyPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(reportService.exportPartyPdf(from, to), PDF,
                "party-report-" + from + "-" + to + ".pdf");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> download(byte[] data, String contentType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
