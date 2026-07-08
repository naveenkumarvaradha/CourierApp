package com.courierapp.service.impl;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.report.ReportSummaryResponse;
import com.courierapp.entity.AuditLog;
import com.courierapp.entity.Booking;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.Party;
import com.courierapp.entity.User;
import com.courierapp.enums.BookingStatus;
import com.courierapp.exception.BusinessException;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.repository.AuditLogRepository;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.CompanySettingsRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.service.ReportService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final AuditLogRepository auditLogRepository;
    private final CompanySettingsRepository companySettingsRepository;

    public ReportServiceImpl(BookingRepository bookingRepository, BookingMapper bookingMapper,
                             UserRepository userRepository, PartyRepository partyRepository,
                             AuditLogRepository auditLogRepository,
                             CompanySettingsRepository companySettingsRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
        this.auditLogRepository = auditLogRepository;
        this.companySettingsRepository = companySettingsRepository;
    }

    // ─── Summary / existing booking report ───────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(String granularity, LocalDate from, LocalDate to) {
        DateRange range = resolveRange(granularity, from, to);
        List<Booking> bookings = bookingRepository.findByBookingDateBetween(range.from(), range.to());
        return buildSummary(range, granularity, bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel(String granularity, LocalDate from, LocalDate to) {
        DateRange range = resolveRange(granularity, from, to);
        List<Booking> bookings = bookingRepository.findByBookingDateBetween(range.from(), range.to());
        ReportSummaryResponse summary = buildSummary(range, granularity, bookings);
        return writeBookingSummaryWorkbook(summary, bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBookingPdf(LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepository.findByBookingDateBetween(from, to);
        return buildBookingDetailPdfBytes("All Statuses", from, to, bookings);
    }

    // ─── User Creation report ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUserCreationExcel(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<User> users = userRepository.findByCreatedAtBetween(f, t);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("User Creation Report");
            CellStyle hdr = boldStyle(wb, new Color(0x1F, 0x49, 0x7D), Color.WHITE);
            String[] cols = {"#", "Username", "Full Name", "Email", "Phone", "Department",
                    "Roles", "Active", "Created By", "Created At"};
            writeExcelHeader(sheet, hdr, cols);
            int r = 1;
            for (User u : users) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(u.getUsername());
                row.createCell(2).setCellValue(u.getFullName());
                row.createCell(3).setCellValue(u.getEmail());
                row.createCell(4).setCellValue(nvl(u.getPhone()));
                row.createCell(5).setCellValue(u.getDepartment() != null ? u.getDepartment().getName() : "—");
                row.createCell(6).setCellValue(u.getRoles().stream().map(ro -> ro.getName()).collect(Collectors.joining(", ")));
                row.createCell(7).setCellValue(u.isActive() ? "Yes" : "No");
                row.createCell(8).setCellValue(nvl(u.getCreatedBy()));
                row.createCell(9).setCellValue(u.getCreatedAt() != null ? DT_FMT.format(u.getCreatedAt()) : "—");
            }
            autoSize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new IllegalStateException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUserCreationPdf(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<User> users = userRepository.findByCreatedAtBetween(f, t);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        addPdfTitle(doc, "User Creation Report", from, to);
        String[] headers = {"#", "Username", "Full Name", "Email", "Department", "Roles", "Active", "Created By", "Created At"};
        float[] widths = {3, 10, 14, 16, 10, 14, 6, 10, 12};
        PdfPTable table = pdfTable(headers, widths);
        int i = 1;
        for (User u : users) {
            addPdfRow(table,
                    String.valueOf(i++),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getDepartment() != null ? u.getDepartment().getName() : "—",
                    u.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(", ")),
                    u.isActive() ? "Yes" : "No",
                    nvl(u.getCreatedBy()),
                    u.getCreatedAt() != null ? DT_FMT.format(u.getCreatedAt()) : "—");
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ─── User Inactive / Disabled report ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUserInactiveExcel(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<User> users = userRepository.findByInactiveAtBetween(f, t);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("User Inactive Report");
            CellStyle hdr = boldStyle(wb, new Color(0x7B, 0x26, 0x26), Color.WHITE);
            String[] cols = {"#", "Username", "Full Name", "Email", "Phone", "Department",
                    "Roles", "Created By", "Created At", "Disabled At", "Updated By"};
            writeExcelHeader(sheet, hdr, cols);
            int r = 1;
            for (User u : users) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(u.getUsername());
                row.createCell(2).setCellValue(u.getFullName());
                row.createCell(3).setCellValue(u.getEmail());
                row.createCell(4).setCellValue(nvl(u.getPhone()));
                row.createCell(5).setCellValue(u.getDepartment() != null ? u.getDepartment().getName() : "—");
                row.createCell(6).setCellValue(u.getRoles().stream().map(ro -> ro.getName()).collect(Collectors.joining(", ")));
                row.createCell(7).setCellValue(nvl(u.getCreatedBy()));
                row.createCell(8).setCellValue(u.getCreatedAt() != null ? DT_FMT.format(u.getCreatedAt()) : "—");
                row.createCell(9).setCellValue(u.getInactiveAt() != null ? DT_FMT.format(u.getInactiveAt()) : "—");
                row.createCell(10).setCellValue(nvl(u.getUpdatedBy()));
            }
            autoSize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new IllegalStateException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUserInactivePdf(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<User> users = userRepository.findByInactiveAtBetween(f, t);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        addPdfTitle(doc, "User Inactive/Disabled Report", from, to);
        String[] headers = {"#", "Username", "Full Name", "Email", "Department", "Created By", "Created At", "Disabled At", "Updated By"};
        float[] widths = {3, 10, 14, 16, 10, 10, 12, 12, 10};
        PdfPTable table = pdfTable(headers, widths);
        int i = 1;
        for (User u : users) {
            addPdfRow(table,
                    String.valueOf(i++),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getDepartment() != null ? u.getDepartment().getName() : "—",
                    nvl(u.getCreatedBy()),
                    u.getCreatedAt() != null ? DT_FMT.format(u.getCreatedAt()) : "—",
                    u.getInactiveAt() != null ? DT_FMT.format(u.getInactiveAt()) : "—",
                    nvl(u.getUpdatedBy()));
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ─── Party / Master report ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPartyExcel(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Party> parties = partyRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(f) && p.getCreatedAt().isBefore(t))
                .sorted(Comparator.comparing(Party::getCreatedAt))
                .collect(Collectors.toList());

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Party details sheet
            Sheet sheet = wb.createSheet("Party Details");
            CellStyle hdr = boldStyle(wb, new Color(0x1F, 0x72, 0x50), Color.WHITE);
            String[] cols = {"#", "Code", "Name", "Company", "Type", "Address", "City", "State", "Pincode",
                    "Phone", "Email", "GSTIN", "Status", "Created By", "Created At", "Updated By", "Updated At"};
            writeExcelHeader(sheet, hdr, cols);
            int r = 1;
            for (Party p : parties) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(p.getPartyCode());
                row.createCell(2).setCellValue(p.getPartyName());
                row.createCell(3).setCellValue(p.getCompanyName() != null ? p.getCompanyName() : "—");
                row.createCell(4).setCellValue(p.getPartyType().name());
                row.createCell(5).setCellValue(p.getAddressLine1() + (p.getAddressLine2() != null ? ", " + p.getAddressLine2() : ""));
                row.createCell(6).setCellValue(p.getCity());
                row.createCell(7).setCellValue(p.getState());
                row.createCell(8).setCellValue(p.getPincode());
                row.createCell(9).setCellValue(nvl(p.getPhone()));
                row.createCell(10).setCellValue(nvl(p.getEmail()));
                row.createCell(11).setCellValue(nvl(p.getGstin()));
                row.createCell(12).setCellValue(p.getPartyStatus() != null ? p.getPartyStatus().name() : (p.isActive() ? "ACTIVE" : "INACTIVE"));
                row.createCell(13).setCellValue(nvl(p.getCreatedBy()));
                row.createCell(14).setCellValue(p.getCreatedAt() != null ? DT_FMT.format(p.getCreatedAt()) : "—");
                row.createCell(15).setCellValue(nvl(p.getUpdatedBy()));
                row.createCell(16).setCellValue(p.getUpdatedAt() != null ? DT_FMT.format(p.getUpdatedAt()) : "—");
            }
            autoSize(sheet, cols.length);

            // Audit log sheet
            Sheet auditSheet = wb.createSheet("Audit Log");
            CellStyle ahdr = boldStyle(wb, new Color(0x55, 0x55, 0x55), Color.WHITE);
            String[] acols = {"Party Code", "Party Name", "Action", "Performed By", "Details", "Date & Time"};
            writeExcelHeader(auditSheet, ahdr, acols);
            int ar = 1;
            for (Party p : parties) {
                List<AuditLog> logs = auditLogRepository.findByModuleAndEntityIdOrderByCreatedAtAsc("PARTY", p.getId());
                for (AuditLog log : logs) {
                    Row row = auditSheet.createRow(ar++);
                    row.createCell(0).setCellValue(p.getPartyCode());
                    row.createCell(1).setCellValue(p.getPartyName());
                    row.createCell(2).setCellValue(log.getAction());
                    row.createCell(3).setCellValue(log.getPerformedBy());
                    row.createCell(4).setCellValue(nvl(log.getDetails()));
                    row.createCell(5).setCellValue(log.getCreatedAt() != null ? DT_FMT.format(log.getCreatedAt()) : "—");
                }
            }
            autoSize(auditSheet, acols.length);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new IllegalStateException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPartyPdf(LocalDate from, LocalDate to) {
        Instant f = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant t = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Party> parties = partyRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(f) && p.getCreatedAt().isBefore(t))
                .sorted(Comparator.comparing(Party::getCreatedAt))
                .collect(Collectors.toList());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A3.rotate(), 20, 20, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        addPdfTitle(doc, "Master (Party) Report", from, to);

        String[] headers = {"#", "Code", "Name", "Company", "Type", "City", "State", "Phone", "Status", "Created By", "Created At"};
        float[] widths = {3, 8, 14, 14, 7, 9, 9, 9, 8, 9, 10};
        PdfPTable table = pdfTable(headers, widths);
        int i = 1;
        for (Party p : parties) {
            addPdfRow(table,
                    String.valueOf(i++),
                    p.getPartyCode(),
                    p.getPartyName(),
                    p.getCompanyName() != null ? p.getCompanyName() : "—",
                    p.getPartyType().name(),
                    p.getCity(),
                    p.getState(),
                    nvl(p.getPhone()),
                    p.getPartyStatus() != null ? p.getPartyStatus().name() : (p.isActive() ? "ACTIVE" : "INACTIVE"),
                    nvl(p.getCreatedBy()),
                    p.getCreatedAt() != null ? DT_FMT.format(p.getCreatedAt()) : "—");
        }
        doc.add(table);

        // Audit logs per party
        for (Party p : parties) {
            List<AuditLog> logs = auditLogRepository.findByModuleAndEntityIdOrderByCreatedAtAsc("PARTY", p.getId());
            if (logs.isEmpty()) continue;
            doc.add(new Paragraph("\nAudit History: " + p.getPartyCode() + " — " + p.getPartyName(),
                    new Font(Font.HELVETICA, 10, Font.BOLD)));
            String[] ah = {"Action", "Performed By", "Details", "Date & Time"};
            float[] aw = {12, 14, 50, 14};
            PdfPTable at = pdfTable(ah, aw);
            for (AuditLog log : logs) {
                addPdfRow(at, log.getAction(), log.getPerformedBy(),
                        nvl(log.getDetails()), log.getCreatedAt() != null ? DT_FMT.format(log.getCreatedAt()) : "—");
            }
            doc.add(at);
        }
        doc.close();
        return out.toByteArray();
    }

    // ─── Booking Detail report ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBookingDetailExcel(LocalDate from, LocalDate to, String status) {
        List<Booking> bookings = fetchBookings(from, to, status);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Bookings sheet
            Sheet sheet = wb.createSheet("Bookings");
            CellStyle hdr = boldStyle(wb, new Color(0x1F, 0x49, 0x7D), Color.WHITE);
            String[] cols = {"#", "Booking No", "Date", "Status", "AWB No", "Mode", "Way",
                    "Sender", "Receiver", "Item Desc", "Weight (kg)", "Packages",
                    "Freight", "Total Charges", "Payment", "Special Instructions",
                    "Created By", "Created At", "Updated By", "Updated At"};
            writeExcelHeader(sheet, hdr, cols);
            int r = 1;
            for (Booking b : bookings) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(r - 1);
                row.createCell(1).setCellValue(b.getBookingNumber());
                row.createCell(2).setCellValue(b.getBookingDate().format(D_FMT));
                row.createCell(3).setCellValue(b.getStatus().name());
                row.createCell(4).setCellValue(nvl(b.getAwbNumber()));
                row.createCell(5).setCellValue(b.getCourierMode().name());
                row.createCell(6).setCellValue(b.getCourierWay() != null ? b.getCourierWay().getName() : "—");
                row.createCell(7).setCellValue(b.getSender().getPartyName());
                row.createCell(8).setCellValue(b.getReceiver().getPartyName());
                row.createCell(9).setCellValue(b.getItemDescription());
                row.createCell(10).setCellValue(b.getWeightKg() != null ? b.getWeightKg().doubleValue() : 0);
                row.createCell(11).setCellValue(b.getNoOfPackages() != null ? b.getNoOfPackages() : 0);
                row.createCell(12).setCellValue(nz(b.getFreightCharges()).doubleValue());
                row.createCell(13).setCellValue(nz(b.getTotalCharges()).doubleValue());
                row.createCell(14).setCellValue(b.getPaymentMode() != null ? b.getPaymentMode().name() : "—");
                row.createCell(15).setCellValue(nvl(b.getSpecialInstructions()));
                row.createCell(16).setCellValue(nvl(b.getCreatedBy()));
                row.createCell(17).setCellValue(b.getCreatedAt() != null ? DT_FMT.format(b.getCreatedAt()) : "—");
                row.createCell(18).setCellValue(nvl(b.getUpdatedBy()));
                row.createCell(19).setCellValue(b.getUpdatedAt() != null ? DT_FMT.format(b.getUpdatedAt()) : "—");
            }
            autoSize(sheet, cols.length);

            // Audit log sheet
            Sheet auditSheet = wb.createSheet("Audit Log");
            CellStyle ahdr = boldStyle(wb, new Color(0x55, 0x55, 0x55), Color.WHITE);
            String[] acols = {"Booking No", "Action", "Performed By", "Details", "Date & Time"};
            writeExcelHeader(auditSheet, ahdr, acols);
            int ar = 1;
            for (Booking b : bookings) {
                List<AuditLog> logs = auditLogRepository.findByModuleAndEntityIdOrderByCreatedAtAsc("BOOKING", b.getId());
                for (AuditLog log : logs) {
                    Row row = auditSheet.createRow(ar++);
                    row.createCell(0).setCellValue(b.getBookingNumber());
                    row.createCell(1).setCellValue(log.getAction());
                    row.createCell(2).setCellValue(log.getPerformedBy());
                    row.createCell(3).setCellValue(nvl(log.getDetails()));
                    row.createCell(4).setCellValue(log.getCreatedAt() != null ? DT_FMT.format(log.getCreatedAt()) : "—");
                }
            }
            autoSize(auditSheet, acols.length);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new IllegalStateException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBookingDetailPdf(LocalDate from, LocalDate to, String status) {
        List<Booking> bookings = fetchBookings(from, to, status);
        String label = status == null || status.isBlank() ? "All Statuses" : status;
        return buildBookingDetailPdfBytes(label, from, to, bookings);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private List<Booking> fetchBookings(LocalDate from, LocalDate to, String status) {
        if (status == null || status.isBlank()) {
            return bookingRepository.findByBookingDateBetween(from, to);
        }
        BookingStatus bs;
        try {
            bs = BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown booking status: " + status);
        }
        return bookingRepository.findByBookingDateBetweenAndStatusOrderByBookingDateAsc(from, to, bs);
    }

    private byte[] buildBookingDetailPdfBytes(String statusLabel, LocalDate from, LocalDate to, List<Booking> bookings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A3.rotate(), 20, 20, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();
        addPdfTitle(doc, "Booking Detail Report — " + statusLabel, from, to);

        String[] headers = {"#", "Booking No", "Date", "Status", "AWB No", "Mode", "Sender", "Receiver",
                "Weight", "Pkgs", "Freight", "Total", "Payment", "Created By", "Created At"};
        float[] widths = {2, 12, 8, 9, 9, 7, 12, 12, 5, 4, 7, 7, 7, 9, 11};
        PdfPTable table = pdfTable(headers, widths);
        int i = 1;
        for (Booking b : bookings) {
            addPdfRow(table,
                    String.valueOf(i++),
                    b.getBookingNumber(),
                    b.getBookingDate().format(D_FMT),
                    b.getStatus().name(),
                    nvl(b.getAwbNumber()),
                    b.getCourierMode().name(),
                    b.getSender().getPartyName(),
                    b.getReceiver().getPartyName(),
                    nz(b.getWeightKg()).toPlainString(),
                    String.valueOf(b.getNoOfPackages() != null ? b.getNoOfPackages() : 0),
                    nz(b.getFreightCharges()).toPlainString(),
                    nz(b.getTotalCharges()).toPlainString(),
                    b.getPaymentMode() != null ? b.getPaymentMode().name() : "—",
                    nvl(b.getCreatedBy()),
                    b.getCreatedAt() != null ? DT_FMT.format(b.getCreatedAt()) : "—");
        }
        doc.add(table);

        // Per-booking audit log
        for (Booking b : bookings) {
            List<AuditLog> logs = auditLogRepository.findByModuleAndEntityIdOrderByCreatedAtAsc("BOOKING", b.getId());
            if (logs.isEmpty()) continue;
            doc.add(new Paragraph("\nAudit: " + b.getBookingNumber(),
                    new Font(Font.HELVETICA, 9, Font.BOLD)));
            String[] ah = {"Action", "Performed By", "Details", "Date & Time"};
            float[] aw = {12, 14, 50, 14};
            PdfPTable at = pdfTable(ah, aw);
            for (AuditLog log : logs) {
                addPdfRow(at, log.getAction(), log.getPerformedBy(),
                        nvl(log.getDetails()), log.getCreatedAt() != null ? DT_FMT.format(log.getCreatedAt()) : "—");
            }
            doc.add(at);
        }
        doc.close();
        return out.toByteArray();
    }

    // ─── Existing summary helpers ─────────────────────────────────────────────

    private ReportSummaryResponse buildSummary(DateRange range, String granularity, List<Booking> bookings) {
        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalFreight = BigDecimal.ZERO;
        BigDecimal totalDeclared = BigDecimal.ZERO;
        Map<String, Long> countByStatus = new TreeMap<>();
        Map<String, Long> countByMode = new TreeMap<>();
        Map<String, BigDecimal> chargesByMode = new TreeMap<>();
        Map<String, PartyAgg> senderAgg = new LinkedHashMap<>();
        Map<String, PartyAgg> receiverAgg = new LinkedHashMap<>();

        for (Booking b : bookings) {
            totalCharges = totalCharges.add(nz(b.getTotalCharges()));
            totalFreight = totalFreight.add(nz(b.getFreightCharges()));
            totalDeclared = totalDeclared.add(nz(b.getDeclaredValue()));
            countByStatus.merge(b.getStatus().name(), 1L, Long::sum);
            String mode = b.getCourierMode().name();
            countByMode.merge(mode, 1L, Long::sum);
            chargesByMode.merge(mode, nz(b.getTotalCharges()), BigDecimal::add);
            aggregate(senderAgg, b.getSender().getPartyCode(), b.getSender().getPartyName(), b.getTotalCharges());
            aggregate(receiverAgg, b.getReceiver().getPartyCode(), b.getReceiver().getPartyName(), b.getTotalCharges());
        }

        List<BookingResponse> bookingDtos = bookings.stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());

        return new ReportSummaryResponse(
                range.from(), range.to(), granularity == null ? "custom" : granularity.toLowerCase(),
                bookings.size(), totalCharges, totalFreight, totalDeclared,
                countByStatus, countByMode, chargesByMode,
                toBreakdownList(senderAgg), toBreakdownList(receiverAgg), bookingDtos);
    }

    private void aggregate(Map<String, PartyAgg> map, String code, String name, BigDecimal charges) {
        PartyAgg agg = map.computeIfAbsent(code, k -> new PartyAgg(code, name));
        agg.count++;
        agg.charges = agg.charges.add(nz(charges));
    }

    private List<ReportSummaryResponse.PartyBreakdown> toBreakdownList(Map<String, PartyAgg> map) {
        return map.values().stream()
                .sorted(Comparator.comparing((PartyAgg a) -> a.charges).reversed())
                .map(a -> new ReportSummaryResponse.PartyBreakdown(a.code, a.name, a.count, a.charges))
                .toList();
    }

    private DateRange resolveRange(String granularity, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        String g = granularity == null ? "custom" : granularity.toLowerCase(Locale.ROOT);
        return switch (g) {
            case "weekly"  -> new DateRange(today.minusDays(6), today);
            case "monthly" -> new DateRange(today.withDayOfMonth(1), today);
            case "yearly"  -> new DateRange(today.withDayOfYear(1), today);
            case "custom" -> {
                if (from == null || to == null)
                    throw new BusinessException("Custom report requires both 'from' and 'to' dates");
                if (from.isAfter(to))
                    throw new BusinessException("'from' date must not be after 'to' date");
                yield new DateRange(from, to);
            }
            default -> throw new BusinessException("Unknown granularity: " + granularity);
        };
    }

    private byte[] writeBookingSummaryWorkbook(ReportSummaryResponse summary, List<Booking> bookings) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle hdr = boldStyle(wb, new Color(0x1F, 0x49, 0x7D), Color.WHITE);

            Sheet summarySheet = wb.createSheet("Summary");
            int r = 0;
            r = writeKV(summarySheet, r, hdr, "Report Range", summary.fromDate() + " to " + summary.toDate());
            r = writeKV(summarySheet, r, hdr, "Granularity", summary.granularity());
            r = writeKV(summarySheet, r, hdr, "Total Bookings", String.valueOf(summary.totalBookings()));
            r = writeKV(summarySheet, r, hdr, "Total Charges", summary.totalCharges().toPlainString());
            r = writeKV(summarySheet, r, hdr, "Total Freight", summary.totalFreight().toPlainString());
            r++;
            r = writeMapSection(summarySheet, r, hdr, "By Status", summary.countByStatus());
            r++;
            writeMapSection(summarySheet, r, hdr, "By Mode (count)", summary.countByMode());
            autoSize(summarySheet, 2);

            Sheet sheet = wb.createSheet("Bookings");
            String[] cols = {"Booking No", "Date", "Status", "Mode", "Sender", "Receiver",
                    "Weight (kg)", "Packages", "Freight", "Total Charges", "Payment"};
            writeExcelHeader(sheet, hdr, cols);
            int ri = 1;
            for (Booking b : bookings) {
                Row row = sheet.createRow(ri++);
                row.createCell(0).setCellValue(b.getBookingNumber());
                row.createCell(1).setCellValue(String.valueOf(b.getBookingDate()));
                row.createCell(2).setCellValue(b.getStatus().name());
                row.createCell(3).setCellValue(b.getCourierMode().name());
                row.createCell(4).setCellValue(b.getSender().getPartyName());
                row.createCell(5).setCellValue(b.getReceiver().getPartyName());
                row.createCell(6).setCellValue(b.getWeightKg() != null ? b.getWeightKg().doubleValue() : 0);
                row.createCell(7).setCellValue(b.getNoOfPackages() != null ? b.getNoOfPackages() : 0);
                row.createCell(8).setCellValue(nz(b.getFreightCharges()).doubleValue());
                row.createCell(9).setCellValue(nz(b.getTotalCharges()).doubleValue());
                row.createCell(10).setCellValue(b.getPaymentMode() != null ? b.getPaymentMode().name() : "—");
            }
            autoSize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Excel report", e);
        }
    }

    // ─── PDF helpers ──────────────────────────────────────────────────────────

    private void addPdfTitle(Document doc, String title, LocalDate from, LocalDate to) {
        try {
            CompanySettings cs = companySettingsRepository.findAll().stream().findFirst().orElse(null);
            String companyName = (cs != null && cs.getCompanyName() != null) ? cs.getCompanyName() : "ShipDesk";
            boolean hasLogo = cs != null && cs.getLogoData() != null && cs.getLogoData().length > 0;

            Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font coFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font subFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            if (hasLogo) {
                // 2-column header: logo left, company + title right
                PdfPTable hdr = new PdfPTable(new float[]{12, 88});
                hdr.setWidthPercentage(100);
                hdr.setSpacingAfter(4);

                com.lowagie.text.pdf.PdfPCell logoCell = new com.lowagie.text.pdf.PdfPCell();
                logoCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                logoCell.setPadding(2);
                try {
                    Image logo = Image.getInstance(cs.getLogoData());
                    logo.scaleToFit(60, 40);
                    logoCell.addElement(logo);
                } catch (Exception ignored) {}
                hdr.addCell(logoCell);

                com.lowagie.text.pdf.PdfPCell textCell = new com.lowagie.text.pdf.PdfPCell();
                textCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                textCell.setPadding(2);
                Paragraph coP = new Paragraph(companyName + "\n", coFont);
                coP.add(new com.lowagie.text.Chunk(title + "\n", titleFont));
                coP.add(new com.lowagie.text.Chunk(
                        "Period: " + from.format(D_FMT) + " to " + to.format(D_FMT)
                        + "  |  Generated: " + LocalDate.now().format(D_FMT), subFont));
                textCell.addElement(coP);
                hdr.addCell(textCell);
                doc.add(hdr);
            } else {
                doc.add(new Paragraph(companyName + " — " + title, titleFont));
                doc.add(new Paragraph("Period: " + from.format(D_FMT) + " to " + to.format(D_FMT)
                        + "  |  Generated: " + LocalDate.now().format(D_FMT), subFont));
            }
            doc.add(new Paragraph(" "));
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
    }

    private PdfPTable pdfTable(String[] headers, float[] relWidths) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        try { table.setWidths(relWidths); } catch (DocumentException ignored) {}
        Font hFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new Color(0x1F, 0x49, 0x7D));
            cell.setPadding(4);
            table.addCell(cell);
        }
        return table;
    }

    private void addPdfRow(PdfPTable table, String... values) {
        Font f = new Font(Font.HELVETICA, 7);
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v == null ? "—" : v, f));
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    // ─── Excel helpers ────────────────────────────────────────────────────────

    private CellStyle boldStyle(Workbook wb, Color ignored1, Color ignored2) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeExcelHeader(Sheet sheet, CellStyle style, String[] cols) {
        Row head = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = head.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
        }
    }

    private int writeKV(Sheet sheet, int rowIdx, CellStyle style, String key, String value) {
        Row row = sheet.createRow(rowIdx);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(style);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private int writeMapSection(Sheet sheet, int rowIdx, CellStyle style, String title, Map<String, Long> data) {
        Row titleRow = sheet.createRow(rowIdx++);
        Cell t = titleRow.createCell(0);
        t.setCellValue(title);
        t.setCellStyle(style);
        for (Map.Entry<String, Long> e : data.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
        return rowIdx;
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String nvl(String v) { return v == null ? "—" : v; }

    private record DateRange(LocalDate from, LocalDate to) {}

    private static final class PartyAgg {
        final String code, name;
        long count;
        BigDecimal charges = BigDecimal.ZERO;
        PartyAgg(String code, String name) { this.code = code; this.name = name; }
    }
}
