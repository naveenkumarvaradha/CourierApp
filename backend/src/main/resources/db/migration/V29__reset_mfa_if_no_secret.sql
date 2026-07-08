-- Safety: if mfa_enabled=true but no secret was ever generated, reset to disabled
UPDATE users SET mfa_enabled = FALSE WHERE mfa_secret IS NULL AND mfa_enabled = TRUE;
