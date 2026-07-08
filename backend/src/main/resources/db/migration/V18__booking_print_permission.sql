-- Add BOOKING_PRINT permission for sticker/label printing
INSERT INTO permissions (module, action, code, description)
VALUES ('BOOKING', 'PRINT', 'BOOKING_PRINT', 'Print shipping label / sticker for a booking');

-- Grant BOOKING_PRINT to the built-in ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code = 'BOOKING_PRINT'
ON CONFLICT DO NOTHING;

-- Grant BOOKING_PRINT to BOOKING_CREATOR role (they need to print what they create)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BOOKING_CREATOR'
  AND p.code = 'BOOKING_PRINT'
ON CONFLICT DO NOTHING;

-- Grant BOOKING_PRINT to APPROVER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'APPROVER'
  AND p.code = 'BOOKING_PRINT'
ON CONFLICT DO NOTHING;
