-- DC receipt sequence (mirrors dc_sequence)
CREATE TABLE dc_receipt_sequence (
    seq_date   VARCHAR(8) PRIMARY KEY,
    last_value BIGINT NOT NULL
);

-- DC Receipts — confirms a Returnable DC has come back; one per DC
CREATE TABLE dc_receipts (
    id                 BIGSERIAL    PRIMARY KEY,
    receipt_number     VARCHAR(40)  NOT NULL,
    receipt_date       DATE         NOT NULL,
    dc_id              BIGINT       NOT NULL REFERENCES delivery_challans(id) ON DELETE RESTRICT,
    previous_dc_status VARCHAR(20)  NOT NULL,
    received_by        VARCHAR(60),
    remarks            VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(100),
    CONSTRAINT uk_dc_receipt_number UNIQUE (receipt_number),
    CONSTRAINT uk_dc_receipt_dc UNIQUE (dc_id)
);

-- Permissions for the new RECEIPT module
INSERT INTO permissions (module, action, code, description) VALUES
    ('RECEIPT', 'VIEW',   'RECEIPT_VIEW',   'View DC receipts'),
    ('RECEIPT', 'CREATE', 'RECEIPT_CREATE', 'Confirm receipt of a returned DC'),
    ('RECEIPT', 'DELETE', 'RECEIPT_DELETE', 'Undo a DC receipt confirmation');

-- Grant to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('RECEIPT_VIEW', 'RECEIPT_CREATE', 'RECEIPT_DELETE')
ON CONFLICT DO NOTHING;

-- Grant to BOOKING_CLERK (create/view; not delete — undo reserved for admins)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BOOKING_CLERK'
  AND p.code IN ('RECEIPT_VIEW', 'RECEIPT_CREATE')
ON CONFLICT DO NOTHING;
