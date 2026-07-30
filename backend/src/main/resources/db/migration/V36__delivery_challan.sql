-- Delivery Challan sequence counter (mirrors booking_sequence)
CREATE TABLE dc_sequence (
    seq_date   VARCHAR(8) PRIMARY KEY,
    last_value BIGINT NOT NULL
);

-- Delivery Challans — one per booking
CREATE TABLE delivery_challans (
    id             BIGSERIAL    PRIMARY KEY,
    dc_number      VARCHAR(40)  NOT NULL,
    dc_date        DATE         NOT NULL,
    booking_id     BIGINT       NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    unit_id        BIGINT       NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
    vehicle_number VARCHAR(30),
    driver_name    VARCHAR(100),
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    remarks        VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     VARCHAR(100),
    CONSTRAINT uk_dc_number UNIQUE (dc_number),
    CONSTRAINT uk_dc_booking UNIQUE (booking_id)
);

CREATE INDEX idx_dc_unit_id ON delivery_challans(unit_id);
CREATE INDEX idx_dc_status ON delivery_challans(status);

-- Permissions for the new DELIVERY_CHALLAN module
INSERT INTO permissions (module, action, code, description) VALUES
    ('DELIVERY_CHALLAN', 'CREATE', 'DELIVERY_CHALLAN_CREATE', 'Create delivery challans'),
    ('DELIVERY_CHALLAN', 'VIEW',   'DELIVERY_CHALLAN_VIEW',   'View delivery challans'),
    ('DELIVERY_CHALLAN', 'UPDATE', 'DELIVERY_CHALLAN_UPDATE', 'Update delivery challans'),
    ('DELIVERY_CHALLAN', 'DELETE', 'DELIVERY_CHALLAN_DELETE', 'Delete delivery challans'),
    ('DELIVERY_CHALLAN', 'PRINT',  'DELIVERY_CHALLAN_PRINT',  'Print delivery challan PDF');

-- Grant all DC permissions to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('DELIVERY_CHALLAN_CREATE', 'DELIVERY_CHALLAN_VIEW', 'DELIVERY_CHALLAN_UPDATE',
                 'DELIVERY_CHALLAN_DELETE', 'DELIVERY_CHALLAN_PRINT')
ON CONFLICT DO NOTHING;

-- BOOKING_CLERK: create/view/update/print (mirrors their booking permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BOOKING_CLERK'
  AND p.code IN ('DELIVERY_CHALLAN_CREATE', 'DELIVERY_CHALLAN_VIEW', 'DELIVERY_CHALLAN_UPDATE',
                 'DELIVERY_CHALLAN_PRINT')
ON CONFLICT DO NOTHING;

-- APPROVER: view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'APPROVER'
  AND p.code = 'DELIVERY_CHALLAN_VIEW'
ON CONFLICT DO NOTHING;

-- VIEWER: view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'VIEWER'
  AND p.code = 'DELIVERY_CHALLAN_VIEW'
ON CONFLICT DO NOTHING;
