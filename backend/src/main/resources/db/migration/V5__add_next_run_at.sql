ALTER TABLE monitors ADD COLUMN next_run_at TIMESTAMPTZ;

UPDATE monitors SET next_run_at = NOW() WHERE next_run_at IS NULL;

ALTER TABLE monitors ALTER COLUMN next_run_at SET NOT NULL;
ALTER TABLE monitors ALTER COLUMN next_run_at SET DEFAULT NOW();

CREATE INDEX idx_monitors_due ON monitors (next_run_at) WHERE enabled = TRUE;