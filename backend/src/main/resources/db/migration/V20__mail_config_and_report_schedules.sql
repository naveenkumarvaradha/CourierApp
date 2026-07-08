-- Add SMTP mail config to company_settings
ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS smtp_host         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS smtp_port         INTEGER      DEFAULT 587,
    ADD COLUMN IF NOT EXISTS smtp_username     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS smtp_password     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS smtp_from_name    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS smtp_tls          BOOLEAN      DEFAULT TRUE;

-- Report schedules
CREATE TABLE IF NOT EXISTS report_schedules (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies(id),
    schedule_name    VARCHAR(150) NOT NULL,
    report_type      VARCHAR(50)  NOT NULL,   -- BOOKING_DETAIL | USER_CREATION | USER_INACTIVE | PARTY
    frequency        VARCHAR(20)  NOT NULL,   -- DAILY | WEEKLY | MONTHLY | YEARLY
    day_of_week      INTEGER,                 -- 1=MON..7=SUN (for WEEKLY)
    day_of_month     INTEGER,                 -- 1-28 (for MONTHLY/YEARLY)
    month_of_year    INTEGER,                 -- 1-12 (for YEARLY)
    recipient_emails TEXT         NOT NULL,   -- comma-separated list
    file_format      VARCHAR(10)  NOT NULL DEFAULT 'EXCEL',  -- EXCEL | PDF
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_at      TIMESTAMP WITH TIME ZONE,
    next_run_at      TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);
