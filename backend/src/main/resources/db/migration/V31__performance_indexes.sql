-- Performance indexes for 3000-user load

-- Bookings: most common query patterns
CREATE INDEX IF NOT EXISTS idx_booking_company ON bookings (company_id);
CREATE INDEX IF NOT EXISTS idx_booking_company_date ON bookings (company_id, booking_date DESC);
CREATE INDEX IF NOT EXISTS idx_booking_company_status ON bookings (company_id, status);
CREATE INDEX IF NOT EXISTS idx_booking_created_at ON bookings (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_booking_awb ON bookings (awb_number) WHERE awb_number IS NOT NULL;

-- Parties: company-scoped lookups
CREATE INDEX IF NOT EXISTS idx_party_company ON parties (company_id);
CREATE INDEX IF NOT EXISTS idx_party_company_name ON parties (company_id, party_name);

-- Users: company-scoped lookups
CREATE INDEX IF NOT EXISTS idx_user_company ON users (company_id);
CREATE INDEX IF NOT EXISTS idx_user_active ON users (active) WHERE active = TRUE;

-- Audit logs: time-range queries
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_module_action ON audit_logs (module, action);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON audit_logs (performed_by);

-- Approval routing: fast routing lookups
CREATE INDEX IF NOT EXISTS idx_approval_company ON approval_routing (company_id);
CREATE INDEX IF NOT EXISTS idx_approval_company_role ON approval_routing (company_id, creator_role_id);

-- Password reset tokens: cleanup queries
CREATE INDEX IF NOT EXISTS idx_pwd_reset_expiry ON password_reset_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_user ON password_reset_tokens (user_id);
