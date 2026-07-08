-- Add optional company association to parties
ALTER TABLE parties
    ADD COLUMN company_id BIGINT NULL REFERENCES companies(id) ON DELETE SET NULL;

CREATE INDEX idx_parties_company_id ON parties(company_id);
