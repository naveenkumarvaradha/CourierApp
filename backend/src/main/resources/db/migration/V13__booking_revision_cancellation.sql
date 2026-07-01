-- Add print_taken flag, cancellation fields, and company PO number to bookings
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS print_taken        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cancellation_remarks VARCHAR(500),
    ADD COLUMN IF NOT EXISTS company_po_no      VARCHAR(100);
