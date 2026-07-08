-- Drop the named unique constraint on users.email
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_user_email;
