CREATE TABLE IF NOT EXISTS password_policy (
    id                          BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT REFERENCES companies(id),
    restrict_last_passwords     INTEGER NOT NULL DEFAULT 5,
    password_expiry_days        INTEGER NOT NULL DEFAULT 90,
    expiry_reminder_days        INTEGER NOT NULL DEFAULT 5,
    session_timeout_hours       INTEGER NOT NULL DEFAULT 0,
    session_timeout_minutes     INTEGER NOT NULL DEFAULT 30,
    max_login_attempts          INTEGER NOT NULL DEFAULT 5,
    min_password_length         INTEGER NOT NULL DEFAULT 8,
    require_uppercase           BOOLEAN NOT NULL DEFAULT TRUE,
    require_lowercase           BOOLEAN NOT NULL DEFAULT TRUE,
    require_digit               BOOLEAN NOT NULL DEFAULT TRUE,
    require_special_char        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Insert default policy
INSERT INTO password_policy (restrict_last_passwords, password_expiry_days, expiry_reminder_days,
    session_timeout_hours, session_timeout_minutes, max_login_attempts,
    min_password_length, require_uppercase, require_lowercase, require_digit, require_special_char)
VALUES (5, 90, 5, 0, 30, 5, 8, TRUE, TRUE, TRUE, FALSE)
ON CONFLICT DO NOTHING;
