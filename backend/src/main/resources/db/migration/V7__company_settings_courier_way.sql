-- Company settings (singleton)
CREATE TABLE company_settings (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_name  VARCHAR(200) NOT NULL DEFAULT 'My Company',
    address_line1 VARCHAR(200) NOT NULL DEFAULT '',
    address_line2 VARCHAR(200),
    city          VARCHAR(100) NOT NULL DEFAULT '',
    state         VARCHAR(100) NOT NULL DEFAULT '',
    pincode       VARCHAR(20)  NOT NULL DEFAULT '',
    country       VARCHAR(100) NOT NULL DEFAULT 'India',
    phone         VARCHAR(30),
    email         VARCHAR(150),
    gstin         VARCHAR(20),
    linked_party_id BIGINT REFERENCES parties(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100)
);

-- Seed one row
INSERT INTO company_settings (company_name, address_line1, city, state, pincode, country, created_by)
VALUES ('My Company', 'No. 1, Main Road', 'Chennai', 'Tamil Nadu', '600001', 'India', 'system');

-- Courier ways (DHL, MARUTI, etc.) - admin managed
CREATE TABLE courier_ways (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100)
);

-- Seed initial courier ways
INSERT INTO courier_ways (name, active, created_by) VALUES
    ('DHL',    TRUE, 'system'),
    ('MARUTI', TRUE, 'system');

-- Add courier_way_id to bookings
ALTER TABLE bookings ADD COLUMN courier_way_id BIGINT REFERENCES courier_ways(id);
