-- Companies (multi-tenant entity)
CREATE TABLE companies (
    id           BIGSERIAL    PRIMARY KEY,
    company_code VARCHAR(20)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by   VARCHAR(100),
    CONSTRAINT uq_company_code UNIQUE (company_code)
);

-- Seed a default company from the existing company_settings record (column is company_name)
DO $$
DECLARE v_name TEXT;
BEGIN
    SELECT company_name INTO v_name FROM company_settings LIMIT 1;
    IF v_name IS NULL OR v_name = '' THEN
        v_name := 'Default Company';
    END IF;
    INSERT INTO companies (company_code, name, active, created_by)
    VALUES ('DEFAULT', v_name, TRUE, 'system')
    ON CONFLICT (company_code) DO NOTHING;
END $$;

-- Link users to the default company
ALTER TABLE users ADD COLUMN company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL;
UPDATE users SET company_id = (SELECT id FROM companies WHERE company_code = 'DEFAULT');

-- Link company_settings to company
ALTER TABLE company_settings ADD COLUMN company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE;
UPDATE company_settings SET company_id = (SELECT id FROM companies WHERE company_code = 'DEFAULT');
