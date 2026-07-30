-- Company units (branch offices) — a company can have multiple physical addresses
CREATE TABLE units (
    id            BIGSERIAL    PRIMARY KEY,
    company_id    BIGINT       NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    unit_name     VARCHAR(150) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    pincode       VARCHAR(20)  NOT NULL,
    country       VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    email         VARCHAR(150),
    gstin         VARCHAR(20),
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100),
    CONSTRAINT uk_unit_company_name UNIQUE (company_id, unit_name)
);

CREATE INDEX idx_units_company_id ON units(company_id);

-- Backfill: one default unit per existing company_settings row, copying its current address
INSERT INTO units (company_id, unit_name, address_line1, address_line2, city, state, pincode,
                    country, phone, email, gstin, is_default, active, created_by)
SELECT cs.company_id, cs.company_name, cs.address_line1, cs.address_line2, cs.city, cs.state,
       cs.pincode, cs.country, cs.phone, cs.email, cs.gstin, TRUE, TRUE, 'system'
FROM company_settings cs
WHERE cs.company_id IS NOT NULL;
