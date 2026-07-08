-- Allow multiple users to share the same email address
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
