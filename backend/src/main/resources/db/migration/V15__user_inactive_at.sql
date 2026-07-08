-- Track when a user was deactivated
ALTER TABLE users ADD COLUMN IF NOT EXISTS inactive_at TIMESTAMPTZ;
