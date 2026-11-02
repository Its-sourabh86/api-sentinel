ALTER TABLE monitors ADD COLUMN owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_monitors_owner ON monitors (owner_id);