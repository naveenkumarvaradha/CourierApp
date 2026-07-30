-- DC Booking becomes a fully standalone module — no longer tied to a Courier Booking.
-- Only throwaway test rows exist so far; reset cleanly rather than backfilling new NOT NULL columns.
TRUNCATE TABLE delivery_challans;

ALTER TABLE delivery_challans DROP COLUMN booking_id;

ALTER TABLE delivery_challans
    ADD COLUMN receiver_type    VARCHAR(10)  NOT NULL,
    ADD COLUMN receiver_party_id BIGINT      REFERENCES parties(id),
    ADD COLUMN receiver_unit_id  BIGINT      REFERENCES units(id),
    ADD COLUMN item_description VARCHAR(500) NOT NULL,
    ADD COLUMN weight_kg        NUMERIC(10,3) NOT NULL,
    ADD COLUMN no_of_packages   INT          NOT NULL,
    ADD COLUMN courier_mode     VARCHAR(20)  NOT NULL,
    ADD COLUMN courier_way_id   BIGINT       REFERENCES courier_ways(id),
    ADD COLUMN package_type_id  BIGINT       REFERENCES package_types(id),
    ADD CONSTRAINT chk_dc_receiver_exclusive
        CHECK ((receiver_party_id IS NOT NULL)::int + (receiver_unit_id IS NOT NULL)::int = 1);

CREATE INDEX idx_dc_receiver_party_id ON delivery_challans(receiver_party_id);
CREATE INDEX idx_dc_receiver_unit_id ON delivery_challans(receiver_unit_id);
