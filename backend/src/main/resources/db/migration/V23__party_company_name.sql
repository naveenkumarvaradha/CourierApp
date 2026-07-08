-- Replace company FK with free-text company_name field
ALTER TABLE parties ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);

-- Migrate existing data: copy company name from the linked company
UPDATE parties p
SET company_name = c.name
FROM companies c
WHERE c.id = p.company_id
  AND p.company_name IS NULL;
