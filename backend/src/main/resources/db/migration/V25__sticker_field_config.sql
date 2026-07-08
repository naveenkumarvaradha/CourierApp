-- Sticker field configuration per company
CREATE TABLE IF NOT EXISTS sticker_field_config (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    field_key   VARCHAR(50) NOT NULL,
    label       VARCHAR(100) NOT NULL,
    visible     BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    UNIQUE (company_id, field_key)
);

-- Default entries are seeded on first access; no static seed here.
