-- Add section column to persist drag-and-drop section assignment
ALTER TABLE sticker_field_config
    ADD COLUMN IF NOT EXISTS section VARCHAR(30) NOT NULL DEFAULT 'HEADER';
