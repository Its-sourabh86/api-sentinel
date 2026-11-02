CREATE TABLE incidents (
    id              BIGSERIAL    PRIMARY KEY,
    monitor_id      BIGINT       NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status          VARCHAR(20)  NOT NULL,
    failure_code    VARCHAR(30),
    reason          VARCHAR(500),
    failure_count   INTEGER      NOT NULL,
    opened_at       TIMESTAMPTZ  NOT NULL,
    last_failure_at TIMESTAMPTZ  NOT NULL,
    resolved_at     TIMESTAMPTZ,

    CONSTRAINT chk_incident_resolved_consistent
        CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL))
);

-- Ek monitor ka ek hi OPEN incident ho sakta hai (DB-level deduplication)
CREATE UNIQUE INDEX uq_incidents_one_open_per_monitor
    ON incidents (monitor_id) WHERE status = 'OPEN';

CREATE INDEX idx_incidents_monitor_opened ON incidents (monitor_id, opened_at DESC);
CREATE INDEX idx_incidents_status_opened  ON incidents (status, opened_at DESC);