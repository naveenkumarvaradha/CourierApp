-- Delivery Challan type — Returnable (e.g. tools/samples sent out temporarily) vs
-- Non-Returnable (standard dispatch), a standard distinction for goods-movement documents.
ALTER TABLE delivery_challans
    ADD COLUMN dc_type VARCHAR(20) NOT NULL DEFAULT 'NON_RETURNABLE';
