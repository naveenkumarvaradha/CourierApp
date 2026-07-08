package com.courierapp.service;

import com.courierapp.dto.admin.ReportScheduleRequest;
import com.courierapp.dto.admin.ReportScheduleResponse;
import com.courierapp.entity.Company;
import com.courierapp.entity.ReportSchedule;
import com.courierapp.exception.ResourceNotFoundException;
import com.courierapp.repository.CompanyRepository;
import com.courierapp.repository.ReportScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ReportScheduleService {

    private final ReportScheduleRepository scheduleRepo;
    private final CompanyRepository companyRepo;
    private final ReportService reportService;
    private final EmailService emailService;

    public ReportScheduleService(ReportScheduleRepository scheduleRepo,
                                 CompanyRepository companyRepo,
                                 ReportService reportService,
                                 EmailService emailService) {
        this.scheduleRepo = scheduleRepo;
        this.companyRepo = companyRepo;
        this.reportService = reportService;
        this.emailService = emailService;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> list() {
        Company company = defaultCompany();
        return scheduleRepo.findByCompanyIdOrderByScheduleNameAsc(company.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportScheduleResponse create(ReportScheduleRequest req) {
        Company company = defaultCompany();
        ReportSchedule s = new ReportSchedule();
        apply(s, req);
        s.setCompany(company);
        s.setNextRunAt(calcNextRun(req, ZonedDateTime.now()));
        return toResponse(scheduleRepo.save(s));
    }

    @Transactional
    public ReportScheduleResponse update(Long id, ReportScheduleRequest req) {
        ReportSchedule s = scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportSchedule", id));
        apply(s, req);
        s.setNextRunAt(calcNextRun(req, ZonedDateTime.now()));
        return toResponse(scheduleRepo.save(s));
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepo.deleteById(id);
    }

    // ── Scheduled runner — fires every night at 00:05 ─────────────────────

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void runDueSchedules() {
        Instant now = Instant.now();
        List<ReportSchedule> due = scheduleRepo.findDueSchedules(now);
        log.info("Report scheduler: {} due schedule(s)", due.size());
        for (ReportSchedule s : due) {
            try {
                runSchedule(s);
            } catch (Exception e) {
                log.error("Failed to run report schedule id={}: {}", s.getId(), e.getMessage(), e);
            }
        }
    }

    private void runSchedule(ReportSchedule s) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate today = now.toLocalDate();
        // Default date range: yesterday (1-day window) for DAILY, last 7 for WEEKLY, etc.
        LocalDate from = switch (s.getFrequency()) {
            case "WEEKLY"  -> today.minusWeeks(1);
            case "MONTHLY" -> today.withDayOfMonth(1).minusMonths(1);
            case "YEARLY"  -> today.withDayOfYear(1).minusYears(1);
            default        -> today.minusDays(1); // DAILY
        };
        LocalDate to = today.minusDays(1);

        byte[] data;
        String ext;
        String contentLabel;
        if ("PDF".equalsIgnoreCase(s.getFileFormat())) {
            data = generatePdf(s.getReportType(), from, to);
            ext = "pdf";
        } else {
            data = generateExcel(s.getReportType(), from, to);
            ext = "xlsx";
        }
        contentLabel = s.getReportType().replace("_", " ");

        String subject = "[ShipDesk] " + s.getScheduleName() + " — " + from + " to " + to;
        String body = "<p>Please find attached the <strong>" + contentLabel + "</strong> report for "
                + from + " to " + to + ".</p><p>This is an automated report from ShipDesk.</p>";
        String filename = s.getReportType().toLowerCase() + "_" + from + "_to_" + to + "." + ext;

        for (String email : s.getRecipientEmails().split(",")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) {
                emailService.sendReportEmail(trimmed, subject, body, data, filename);
            }
        }

        s.setLastRunAt(Instant.now());
        s.setNextRunAt(calcNextRun(toRequest(s), ZonedDateTime.now()));
        scheduleRepo.save(s);
        log.info("Report schedule '{}' (id={}) sent to {} recipients",
                s.getScheduleName(), s.getId(), s.getRecipientEmails().split(",").length);
    }

    private byte[] generateExcel(String type, LocalDate from, LocalDate to) {
        return switch (type) {
            case "USER_CREATION" -> reportService.exportUserCreationExcel(from, to);
            case "USER_INACTIVE" -> reportService.exportUserInactiveExcel(from, to);
            case "PARTY"         -> reportService.exportPartyExcel(from, to);
            default              -> reportService.exportBookingDetailExcel(from, to, null);
        };
    }

    private byte[] generatePdf(String type, LocalDate from, LocalDate to) {
        return switch (type) {
            case "USER_CREATION" -> reportService.exportUserCreationPdf(from, to);
            case "USER_INACTIVE" -> reportService.exportUserInactivePdf(from, to);
            case "PARTY"         -> reportService.exportPartyPdf(from, to);
            default              -> reportService.exportBookingDetailPdf(from, to, null);
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Company defaultCompany() {
        return companyRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No company configured"));
    }

    private void apply(ReportSchedule s, ReportScheduleRequest r) {
        s.setScheduleName(r.scheduleName());
        s.setReportType(r.reportType());
        s.setFrequency(r.frequency());
        s.setDayOfWeek(r.dayOfWeek());
        s.setDayOfMonth(r.dayOfMonth());
        s.setMonthOfYear(r.monthOfYear());
        s.setRecipientEmails(r.recipientEmails());
        s.setFileFormat(r.fileFormat() != null ? r.fileFormat() : "EXCEL");
        s.setEnabled(r.enabled());
    }

    private ReportScheduleRequest toRequest(ReportSchedule s) {
        return new ReportScheduleRequest(s.getScheduleName(), s.getReportType(),
                s.getFrequency(), s.getDayOfWeek(), s.getDayOfMonth(), s.getMonthOfYear(),
                s.getRecipientEmails(), s.getFileFormat(), s.isEnabled());
    }

    static Instant calcNextRun(ReportScheduleRequest req, ZonedDateTime from) {
        ZonedDateTime next = switch (req.frequency()) {
            case "WEEKLY" -> {
                int dow = req.dayOfWeek() != null ? req.dayOfWeek() : 1; // default Monday
                DayOfWeek day = DayOfWeek.of(dow);
                ZonedDateTime candidate = from.with(TemporalAdjusters.nextOrSame(day)).withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield candidate.isAfter(from) ? candidate : candidate.plusWeeks(1);
            }
            case "MONTHLY" -> {
                int dom = req.dayOfMonth() != null ? req.dayOfMonth() : 1;
                ZonedDateTime candidate = from.withDayOfMonth(Math.min(dom, from.toLocalDate().lengthOfMonth())).withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield candidate.isAfter(from) ? candidate : candidate.plusMonths(1);
            }
            case "YEARLY" -> {
                int month = req.monthOfYear() != null ? req.monthOfYear() : 1;
                int dom = req.dayOfMonth() != null ? req.dayOfMonth() : 1;
                ZonedDateTime candidate = from.withMonth(month).withDayOfMonth(dom).withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield candidate.isAfter(from) ? candidate : candidate.plusYears(1);
            }
            default -> from.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0); // DAILY
        };
        return next.toInstant();
    }

    private ReportScheduleResponse toResponse(ReportSchedule s) {
        return new ReportScheduleResponse(
                s.getId(), s.getScheduleName(), s.getReportType(), s.getFrequency(),
                s.getDayOfWeek(), s.getDayOfMonth(), s.getMonthOfYear(),
                s.getRecipientEmails(), s.getFileFormat(), s.isEnabled(),
                s.getLastRunAt(), s.getNextRunAt(),
                s.getCreatedBy(), s.getCreatedAt());
    }
}
