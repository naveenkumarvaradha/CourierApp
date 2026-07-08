-- Performance indexes for 3000-user load
-- Only adding indexes not already created in V1-V30

-- Bookings: additional query patterns
CREATE INDEX IF NOT EXISTS idx_booking_created_at ON bookings (created_at DESC);

-- Parties: name search within company (company_id added in V22)
CREATE INDEX IF NOT EXISTS idx_party_company_name ON parties (company_id, party_name);

-- Users: company-scoped lookups (company_id added in V11)
CREATE INDEX IF NOT EXISTS idx_user_company ON users (company_id);
CREATE INDEX IF NOT EXISTS idx_user_active ON users (active) WHERE active = TRUE;
