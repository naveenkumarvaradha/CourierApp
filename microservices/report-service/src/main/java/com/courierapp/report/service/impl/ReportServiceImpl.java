package com.courierapp.report.service.impl;

import com.courierapp.report.dto.ReportSummaryResponse;
import com.courierapp.report.entity.AuditLog;
import com.courierapp.report.entity.Booking;
import com.courierapp.report.entity.Party;
import com.courierapp.report.repository.AuditLogReportRepository;
import com.courierapp.report.repository.BookingReportRepository;
import com.courierapp.report.repository.PartyReportRepository;
import com.courierapp.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BookingReportRepository bookingRepo;
    private final AuditLogReportRepository auditLogRepo;
    private final PartyReportRepository partyRepo;

    @Override
    public ReportSummaryResponse summary(String granularity, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        List<Booking> bookings = bookingRepo.findByDateRange(effectiveFrom, effectiveTo);

        Map<String, Long> byStatus = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus() != null ? b.getStatus() : "UNKNOWN", Collectors.counting()));

        DateTimeFormatter fmt = "monthly".equalsIgnoreCase(granularity)
                ? DateTimeFormatter.ofPattern("yyyy-MM")
                : DateTimeFormatter.ofPattern("yyyy-'W'ww");

        Map<String, List<Booking>> grouped = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getBookingDate() != null ? b.getBookingDate().format(fmt) : "UNKNOWN"));

        List<ReportSummaryResponse.PeriodStat> periodStats = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Long> periodByStatus = e.getValue().stream()
                            .collect(Collectors.groupingBy(b -> b.getStatus() != null ? b.getStatus() : "UNKNOWN", Collectors.counting()));
                    return new ReportSummaryResponse.PeriodStat(e.getKey(), e.getValue().size(), periodByStatus);
                }).toList();

        return new ReportSummaryResponse(granularity, effectiveFrom.toString(), effectiveTo.toString(),
                bookings.size(), byStatus, periodStats);
    }

    @Override
    public byte[] exportExcel(String granularity, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        List<Booking> bookings = bookingRepo.findByDateRange(effectiveFrom, effectiveTo);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Booking Summary");
            Row header = sheet.createRow(0);
            String[] cols = {"Period", "Total", "DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "DISPATCHED", "DELIVERED"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            DateTimeFormatter fmt = "monthly".equalsIgnoreCase(granularity)
                    ? DateTimeFormatter.ofPattern("yyyy-MM") : DateTimeFormatter.ofPattern("yyyy-'W'ww");
            Map<String, List<Booking>> grouped = bookings.stream()
                    .collect(Collectors.groupingBy(b -> b.getBookingDate() != null ? b.getBookingDate().format(fmt) : "UNKNOWN"));
            int rowNum = 1;
            for (var entry : new TreeMap<>(grouped).entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().size());
                Map<String, Long> s = entry.getValue().stream().collect(Collectors.groupingBy(b -> b.getStatus() != null ? b.getStatus() : "", Collectors.counting()));
                row.createCell(2).setCellValue(s.getOrDefault("DRAFT", 0L));
                row.createCell(3).setCellValue(s.getOrDefault("PENDING_APPROVAL", 0L));
                row.createCell(4).setCellValue(s.getOrDefault("APPROVED", 0L));
                row.createCell(5).setCellValue(s.getOrDefault("REJECTED", 0L));
                row.createCell(6).setCellValue(s.getOrDefault("DISPATCHED", 0L));
                row.createCell(7).setCellValue(s.getOrDefault("DELIVERED", 0L));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel export failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    @Override
    public byte[] exportBookingPdf(LocalDate from, LocalDate to) {
        // TODO: implement full PDF with iText; returning simple stub
        log.info("exportBookingPdf from={} to={}", from, to);
        return generateSimpleBookingPdfStub(from, to);
    }

    @Override
    public byte[] exportBookingDetailExcel(LocalDate from, LocalDate to, String status) {
        List<Booking> bookings = bookingRepo.findByDateRangeAndStatus(from, to, status);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bookings");
            String[] cols = {"Booking No", "Date", "Status", "AWB", "Courier Mode", "Weight (kg)", "Packages", "Created By"};
            Row hdr = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) hdr.createCell(i).setCellValue(cols[i]);
            int rowNum = 1;
            for (Booking b : bookings) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getBookingNumber());
                row.createCell(1).setCellValue(b.getBookingDate() != null ? b.getBookingDate().toString() : "");
                row.createCell(2).setCellValue(b.getStatus());
                row.createCell(3).setCellValue(b.getAwbNumber() != null ? b.getAwbNumber() : "");
                row.createCell(4).setCellValue(b.getCourierMode() != null ? b.getCourierMode() : "");
                row.createCell(5).setCellValue(b.getWeightKg() != null ? b.getWeightKg().doubleValue() : 0);
                row.createCell(6).setCellValue(b.getNoOfPackages() != null ? b.getNoOfPackages() : 0);
                row.createCell(7).setCellValue(b.getCreatedBy() != null ? b.getCreatedBy() : "");
            }
            // Audit log sheet
            Sheet auditSheet = wb.createSheet("Audit Log");
            String[] auditCols = {"Module", "Action", "Entity ID", "Performed By", "Details", "Timestamp"};
            Row auditHdr = auditSheet.createRow(0);
            for (int i = 0; i < auditCols.length; i++) auditHdr.createCell(i).setCellValue(auditCols[i]);
            List<AuditLog> logs = auditLogRepo.findByModuleAndDateRange("BOOKING",
                    from.atStartOfDay(ZoneOffset.UTC).toInstant(), to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            int ar = 1;
            for (AuditLog al : logs) {
                Row row = auditSheet.createRow(ar++);
                row.createCell(0).setCellValue(al.getModule());
                row.createCell(1).setCellValue(al.getAction());
                row.createCell(2).setCellValue(al.getEntityId() != null ? al.getEntityId() : 0);
                row.createCell(3).setCellValue(al.getPerformedBy() != null ? al.getPerformedBy() : "");
                row.createCell(4).setCellValue(al.getDetails() != null ? al.getDetails() : "");
                row.createCell(5).setCellValue(al.getCreatedAt() != null ? al.getCreatedAt().toString() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Booking detail Excel failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    @Override
    public byte[] exportBookingDetailPdf(LocalDate from, LocalDate to, String status) {
        log.info("exportBookingDetailPdf from={} to={} status={}", from, to, status);
        return generateSimpleBookingPdfStub(from, to);
    }

    @Override
    public byte[] exportUserCreationExcel(LocalDate from, LocalDate to) {
        List<AuditLog> logs = auditLogRepo.findByModuleAndDateRange("USER",
                from.atStartOfDay(ZoneOffset.UTC).toInstant(), to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        return buildAuditLogExcel(logs, "User Creation Report");
    }

    @Override
    public byte[] exportUserCreationPdf(LocalDate from, LocalDate to) {
        log.info("exportUserCreationPdf from={} to={}", from, to);
        return generateSimpleBookingPdfStub(from, to);
    }

    @Override
    public byte[] exportUserInactiveExcel(LocalDate from, LocalDate to) {
        List<AuditLog> logs = auditLogRepo.findByModuleAndDateRange("USER",
                from.atStartOfDay(ZoneOffset.UTC).toInstant(), to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                .stream().filter(a -> "DEACTIVATE".equals(a.getAction()) || "DELETE".equals(a.getAction())).toList();
        return buildAuditLogExcel(logs, "Inactive Users Report");
    }

    @Override
    public byte[] exportUserInactivePdf(LocalDate from, LocalDate to) {
        log.info("exportUserInactivePdf from={} to={}", from, to);
        return generateSimpleBookingPdfStub(from, to);
    }

    @Override
    public byte[] exportPartyExcel(LocalDate from, LocalDate to) {
        List<Party> parties = partyRepo.findByCreatedAtRange(
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Parties");
            String[] cols = {"Party Code", "Party Name", "Type", "City", "State", "Phone", "Status", "Created By", "Created At"};
            Row hdr = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) hdr.createCell(i).setCellValue(cols[i]);
            int rowNum = 1;
            for (Party p : parties) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getPartyCode());
                row.createCell(1).setCellValue(p.getPartyName());
                row.createCell(2).setCellValue(p.getPartyType() != null ? p.getPartyType() : "");
                row.createCell(3).setCellValue(p.getCity() != null ? p.getCity() : "");
                row.createCell(4).setCellValue(p.getState() != null ? p.getState() : "");
                row.createCell(5).setCellValue(p.getPhone() != null ? p.getPhone() : "");
                row.createCell(6).setCellValue(p.getPartyStatus() != null ? p.getPartyStatus() : "");
                row.createCell(7).setCellValue(p.getCreatedBy() != null ? p.getCreatedBy() : "");
                row.createCell(8).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Party Excel failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    @Override
    public byte[] exportPartyPdf(LocalDate from, LocalDate to) {
        log.info("exportPartyPdf from={} to={}", from, to);
        return generateSimpleBookingPdfStub(from, to);
    }

    private byte[] buildAuditLogExcel(List<AuditLog> logs, String sheetName) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            String[] cols = {"Module", "Action", "Entity Name", "Performed By", "Details", "Timestamp"};
            Row hdr = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) hdr.createCell(i).setCellValue(cols[i]);
            int rowNum = 1;
            for (AuditLog al : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(al.getModule());
                row.createCell(1).setCellValue(al.getAction());
                row.createCell(2).setCellValue(al.getEntityName() != null ? al.getEntityName() : "");
                row.createCell(3).setCellValue(al.getPerformedBy() != null ? al.getPerformedBy() : "");
                row.createCell(4).setCellValue(al.getDetails() != null ? al.getDetails() : "");
                row.createCell(5).setCellValue(al.getCreatedAt() != null ? al.getCreatedAt().toString() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Audit log Excel failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    private byte[] generateSimpleBookingPdfStub(LocalDate from, LocalDate to) {
        // TODO: Implement full PDF generation with iText
        // Returning minimal valid PDF stub
        try {
            String content = "%PDF-1.4\n1 0 obj<</Type /Catalog /Pages 2 0 R>> endobj\n"
                    + "2 0 obj<</Type /Pages /Kids [3 0 R] /Count 1>> endobj\n"
                    + "3 0 obj<</Type /Page /Parent 2 0 R /Resources<<>> /MediaBox [0 0 595 842] /Contents 4 0 R>> endobj\n"
                    + "4 0 obj<</Length 44>>\nstream\nBT /F1 12 Tf 50 700 Td (Report: " + from + " to " + to + ") Tj ET\nendstream\nendobj\n"
                    + "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000068 00000 n\n0000000125 00000 n\n0000000266 00000 n\n"
                    + "trailer<</Size 5 /Root 1 0 R>>\nstartxref\n338\n%%EOF";
            return content.getBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
