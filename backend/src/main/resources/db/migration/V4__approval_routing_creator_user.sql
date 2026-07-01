-- Add creator_user_id to approval_routing:
-- When set, this routing rule only applies to bookings created by this specific user.
-- creator_user_id takes precedence over creator_role_id if both are set (use one or the other).
ALTER TABLE approval_routing ADD COLUMN creator_user_id BIGINT REFERENCES users(id);
