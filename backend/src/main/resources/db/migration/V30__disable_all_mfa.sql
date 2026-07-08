-- Reset MFA for all users — no one has completed proper setup via the UI yet
UPDATE users SET mfa_enabled = FALSE, mfa_secret = NULL;
