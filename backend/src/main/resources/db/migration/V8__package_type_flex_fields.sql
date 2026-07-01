-- ── Package Types ──────────────────────────────────────────────
CREATE TABLE package_types (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100)
);

INSERT INTO package_types (name, active, created_by) VALUES
    ('BOX',   TRUE, 'system'),
    ('COVER', TRUE, 'system');

-- Add package_type_id to bookings and make charge columns nullable
ALTER TABLE bookings ADD COLUMN package_type_id BIGINT REFERENCES package_types(id);
ALTER TABLE bookings ALTER COLUMN payment_mode    DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN freight_charges DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN total_charges   DROP NOT NULL;

-- ── Flex Fields ─────────────────────────────────────────────────
-- Field definitions: one row per custom field per module
CREATE TABLE flex_fields (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module      VARCHAR(50)  NOT NULL,          -- BOOKING, MASTER_PARTY
    field_name  VARCHAR(100) NOT NULL,
    field_label VARCHAR(200) NOT NULL,
    field_type  VARCHAR(30)  NOT NULL,          -- TEXT, DROPDOWN_SINGLE, DROPDOWN_MULTI
    required    BOOLEAN NOT NULL DEFAULT FALSE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    UNIQUE (module, field_name)
);

-- Dropdown options for flex fields
CREATE TABLE flex_field_options (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    field_id     BIGINT NOT NULL REFERENCES flex_fields(id) ON DELETE CASCADE,
    option_value VARCHAR(200) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Stored values (one row per field per entity)
CREATE TABLE flex_field_values (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module       VARCHAR(50) NOT NULL,
    entity_id    BIGINT      NOT NULL,
    field_id     BIGINT      NOT NULL REFERENCES flex_fields(id) ON DELETE CASCADE,
    field_value  TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (module, entity_id, field_id)
);
