ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS logo_data         BYTEA,
    ADD COLUMN IF NOT EXISTS logo_content_type VARCHAR(100);
