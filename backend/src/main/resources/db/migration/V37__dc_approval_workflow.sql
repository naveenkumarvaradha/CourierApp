-- Delivery Challan approval workflow — mirrors bookings' multi-level approval
ALTER TABLE delivery_challans
    ADD COLUMN current_approval_level INT NOT NULL DEFAULT 1,
    ADD COLUMN approver_username VARCHAR(60),
    ADD COLUMN approval_timestamp TIMESTAMPTZ,
    ADD COLUMN approval_remarks VARCHAR(500);

-- Permission for approving/rejecting delivery challans
INSERT INTO permissions (module, action, code, description)
VALUES ('DELIVERY_CHALLAN', 'APPROVE', 'DELIVERY_CHALLAN_APPROVE', 'Approve or reject delivery challans');

-- Grant to ADMIN and APPROVER roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code = 'DELIVERY_CHALLAN_APPROVE'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'APPROVER'
  AND p.code = 'DELIVERY_CHALLAN_APPROVE'
ON CONFLICT DO NOTHING;

-- Approval routing: APPROVER and ADMIN roles designated as level-1 approvers for DELIVERY_CHALLAN
INSERT INTO approval_routing (role_id, user_id, active, module, level, created_by, created_at)
SELECT (SELECT id FROM roles WHERE name = 'APPROVER'), NULL, TRUE, 'DELIVERY_CHALLAN', 1, 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM approval_routing ar
    JOIN roles r ON r.id = ar.role_id
    WHERE r.name = 'APPROVER' AND ar.module = 'DELIVERY_CHALLAN' AND ar.level = 1
);

INSERT INTO approval_routing (role_id, user_id, active, module, level, created_by, created_at)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'), NULL, TRUE, 'DELIVERY_CHALLAN', 1, 'system', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM approval_routing ar
    JOIN roles r ON r.id = ar.role_id
    WHERE r.name = 'ADMIN' AND ar.module = 'DELIVERY_CHALLAN' AND ar.level = 1
);
