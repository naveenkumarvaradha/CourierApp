-- Multi-level approval support
ALTER TABLE approval_routing ADD COLUMN IF NOT EXISTS level INT NOT NULL DEFAULT 1;
ALTER TABLE bookings        ADD COLUMN IF NOT EXISTS current_approval_level INT NOT NULL DEFAULT 1;
ALTER TABLE parties         ADD COLUMN IF NOT EXISTS current_approval_level INT NOT NULL DEFAULT 1;

COMMENT ON COLUMN approval_routing.level IS '1 = first level, 2 = second level, etc. Approval flows through levels in ascending order.';
