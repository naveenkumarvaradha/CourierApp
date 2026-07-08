-- Add BOOKING_REVISE permission for sending an approved booking back to draft for editing
INSERT INTO permissions (module, action, code, description)
VALUES ('BOOKING', 'REVISE', 'BOOKING_REVISE', 'Revise (re-open) an approved booking for editing');

-- Grant to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.code = 'BOOKING_REVISE'
ON CONFLICT DO NOTHING;

-- Grant to BOOKING_CREATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'BOOKING_CREATOR' AND p.code = 'BOOKING_REVISE'
ON CONFLICT DO NOTHING;
