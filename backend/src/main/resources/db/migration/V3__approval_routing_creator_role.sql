-- Add creator_role_id to approval_routing:
-- When set, this routing rule only applies to bookings created by users who have this role.
-- When NULL, the rule applies to ALL bookings (legacy / catch-all behaviour).
ALTER TABLE approval_routing ADD COLUMN creator_role_id BIGINT REFERENCES roles(id);

-- Reconfigure seed routing:
-- APPROVER role approves bookings created by BOOKING_CLERK role
UPDATE approval_routing
SET creator_role_id = (SELECT id FROM roles WHERE name = 'BOOKING_CLERK')
WHERE role_id = (SELECT id FROM roles WHERE name = 'APPROVER');

-- Remove ADMIN as a general approver (admin manages system but should not approve clerk bookings)
DELETE FROM approval_routing
WHERE role_id = (SELECT id FROM roles WHERE name = 'ADMIN');
