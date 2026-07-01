-- Add approval status to parties
ALTER TABLE parties ADD COLUMN party_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Existing active parties stay ACTIVE; inactive ones become INACTIVE
UPDATE parties SET party_status = CASE WHEN active = true THEN 'ACTIVE' ELSE 'INACTIVE' END;

-- Add module scope to approval_routing (BOOKING or MASTER)
ALTER TABLE approval_routing ADD COLUMN module VARCHAR(30) NOT NULL DEFAULT 'BOOKING';

-- Grant MASTER_APPROVE to APPROVER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'APPROVER'),
       (SELECT id FROM permissions WHERE code = 'MASTER_APPROVE')
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    JOIN roles r ON r.id = rp.role_id
    JOIN permissions p ON p.id = rp.permission_id
    WHERE r.name = 'APPROVER' AND p.code = 'MASTER_APPROVE'
);

-- Add a MASTER-scoped approval routing rule: APPROVER role can approve master data
INSERT INTO approval_routing (role_id, user_id, active, module, created_by, created_at)
VALUES ((SELECT id FROM roles WHERE name = 'APPROVER'), NULL, TRUE, 'MASTER', 'system', NOW());
