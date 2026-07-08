package com.courierapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "report_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSchedule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private com.courierapp.entity.Company company;

    @Column(name = "schedule_name", nullable = false, length = 150)
    private String scheduleName;

    /** BOOKING_DETAIL | USER_CREATION | USER_INACTIVE | PARTY */
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    /** DAILY | WEEKLY | MONTHLY | YEARLY */
    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency;

    /** 1=MON … 7=SUN — used for WEEKLY */
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    /** 1-28 — used for MONTHLY and YEARLY */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /** 1-12 — used for YEARLY */
    @Column(name = "month_of_year")
    private Integer monthOfYear;

    /** Comma-separated email list */
    @Column(name = "recipient_emails", nullable = false, columnDefinition = "TEXT")
    private String recipientEmails;

    /** EXCEL | PDF */
    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;
}
