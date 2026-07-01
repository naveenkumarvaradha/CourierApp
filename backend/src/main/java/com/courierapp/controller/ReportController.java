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

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Booking summary report (JSON). Granularity: weekly|monthly|yearly|custom")
    public ReportSummaryResponse summary(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(granularity, from, to);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    @Operation(summary = "Export booking report as Excel (.xlsx)")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] xlsx = reportService.exportExcel(granularity, from, to);
        String filename = "booking-report-" + granularity + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(XLSX))
                .body(xlsx);
    }
}
