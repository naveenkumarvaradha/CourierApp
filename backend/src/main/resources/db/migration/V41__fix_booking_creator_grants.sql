-- V36/V40 granted DELIVERY_CHALLAN_*/RECEIPT_* permissions to a role named 'BOOKING_CLERK',
-- but this database's actual booking-staff role is named 'BOOKING_CREATOR' (the seed migration's
-- BOOKING_CLERK name never matched here), so those grants silently applied to nobody. Fix by
-- granting the same intended permissions to BOOKING_CREATOR directly.

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BOOKING_CREATOR'
  AND p.code IN ('DELIVERY_CHALLAN_CREATE', 'DELIVERY_CHALLAN_VIEW', 'DELIVERY_CHALLAN_UPDATE',
                 'DELIVERY_CHALLAN_PRINT')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BOOKING_CREATOR'
  AND p.code IN ('RECEIPT_VIEW', 'RECEIPT_CREATE')
ON CONFLICT DO NOTHING;
