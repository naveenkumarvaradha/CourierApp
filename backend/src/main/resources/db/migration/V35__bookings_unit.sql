-- Add optional unit (sending branch) reference to bookings — controls the address
-- printed as shipment origin on the sticker/DC. Null falls back to today's behavior
-- (CompanySettings address).
ALTER TABLE bookings
    ADD COLUMN unit_id BIGINT NULL REFERENCES units(id) ON DELETE SET NULL;

CREATE INDEX idx_bookings_unit_id ON bookings(unit_id);

-- Best-effort backfill: existing bookings get their sender's company's default unit
UPDATE bookings b
SET unit_id = u.id
FROM parties p
JOIN units u ON u.company_id = p.company_id AND u.is_default = TRUE
WHERE b.sender_id = p.id
  AND b.unit_id IS NULL;
