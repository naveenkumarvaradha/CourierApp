-- Change DEFAULT company code to numeric "1"
UPDATE companies SET company_code = '1' WHERE company_code = 'DEFAULT';

-- Link company_settings to company id=1 so booking numbers can pick up the code
UPDATE company_settings
SET company_id = (SELECT id FROM companies WHERE company_code = '1' LIMIT 1)
WHERE company_id IS NULL;
