-- Courier tracking integration: stores tracking events pulled from
-- carrier APIs (DHL, Maruti) keyed to a booking's AWB number.

CREATE TABLE tracking_events (
    id           BIGSERIAL     PRIMARY KEY,
    booking_id   BIGINT        NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    provider     VARCHAR(30)   NOT NULL,
    status       VARCHAR(100)  NOT NULL,
    description  VARCHAR(500),
    location     VARCHAR(255),
    event_time   TIMESTAMPTZ,
    raw_payload  TEXT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tracking_events_booking ON tracking_events(booking_id);
CREATE UNIQUE INDEX uq_tracking_events_dedupe
    ON tracking_events(booking_id, status, COALESCE(event_time, created_at));

ALTER TABLE bookings ADD COLUMN last_tracked_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN tracking_status VARCHAR(100);
