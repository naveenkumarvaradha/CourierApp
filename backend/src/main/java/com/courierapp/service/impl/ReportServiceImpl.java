package com.courierapp.service.impl;

import com.courierapp.dto.booking.BookingResponse;
import com.courierapp.dto.report.ReportSummaryResponse;
import com.courierapp.entity.Booking;
import com.courierapp.exception.BusinessException;
import com.courierapp.mapper.BookingMapper;
import com.courierapp.repository.BookingRepository;
import com.courierapp.service.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    public ReportServiceImpl(BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

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
        return writeWorkbook(summary, bookings);
    }

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
                toBreakdownList(senderAgg), toBreakdownList(receiverAgg),
                bookingDtos);
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
            case "weekly" -> new DateRange(today.minusDays(6), today);
            case "monthly" -> new DateRange(today.withDayOfMonth(1), today);
            case "yearly" -> new DateRange(today.withDayOfYear(1), today);
            case "custom" -> {
                if (from == null || to == null) {
                    throw new BusinessException("Custom report requires both 'from' and 'to' dates");
                }
                if (from.isAfter(to)) {
                    throw new BusinessException("'from' date must not be after 'to' date");
                }
                yield new DateRange(from, to);
            }
            default -> throw new BusinessException("Unknown granularity: " + granularity
                    + " (expected weekly, monthly, yearly or custom)");
        };
    }

    private byte[] writeWorkbook(ReportSummaryResponse summary, List<Booking> bookings) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Summary sheet
            Sheet summarySheet = wb.createSheet("Summary");
            int r = 0;
            r = writeKeyValue(summarySheet, r, headerStyle, "Report Range",
                    summary.fromDate() + " to " + summary.toDate());
            r = writeKeyValue(summarySheet, r, headerStyle, "Granularity", summary.granularity());
            r = writeKeyValue(summarySheet, r, headerStyle, "Total Bookings",
                    String.valueOf(summary.totalBookings()));
            r = writeKeyValue(summarySheet, r, headerStyle, "Total Charges",
                    String.valueOf(summary.totalCharges()));
            r = writeKeyValue(summarySheet, r, headerStyle, "Total Freight",
                    String.valueOf(summary.totalFreight()));
            r++;
            r = writeSectionMap(summarySheet, r, headerStyle, "By Status", summary.countByStatus());
            r++;
            r = writeSectionMap(summarySheet, r, headerStyle, "By Mode (count)", summary.countByMode());
            autoSize(summarySheet, 2);

            // Bookings sheet
            Sheet sheet = wb.createSheet("Bookings");
            String[] cols = {"Booking No", "Date", "Status", "Mode", "Sender", "Receiver",
                    "Weight (kg)", "Packages", "Freight", "Total Charges", "Payment"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (Booking b : bookings) {
                Row row = sheet.createRow(rowIdx++);
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
                row.createCell(10).setCellValue(b.getPaymentMode().name());
            }
            autoSize(sheet, cols.length);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Excel report", e);
        }
    }

    private int writeKeyValue(Sheet sheet, int rowIdx, CellStyle headerStyle, String key, String value) {
        Row row = sheet.createRow(rowIdx);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private int writeSectionMap(Sheet sheet, int rowIdx, CellStyle headerStyle, String title,
                                Map<String, Long> data) {
        Row titleRow = sheet.createRow(rowIdx++);
        Cell t = titleRow.createCell(0);
        t.setCellValue(title);
        t.setCellStyle(headerStyle);
        for (Map.Entry<String, Long> e : data.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
        return rowIdx;
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private static final class PartyAgg {
        final String code;
        final String name;
        long count;
        BigDecimal charges = BigDecimal.ZERO;

        PartyAgg(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
