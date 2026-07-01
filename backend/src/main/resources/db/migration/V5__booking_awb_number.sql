-- AWB (Air Waybill) number — entered by approver after approval, unique per booking.
-- Null until explicitly set; sticker print is blocked until this is populated.
ALTER TABLE bookings ADD COLUMN awb_number VARCHAR(60) UNIQUE;
