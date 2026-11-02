CREATE TABLE check_results (
    id             BIGSERIAL    PRIMARY KEY,
    monitor_id     BIGINT       NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    started_at     TIMESTAMPTZ  NOT NULL,
    duration_ms    BIGINT       NOT NULL,
    http_status    INTEGER,
    result_status  VARCHAR(20)  NOT NULL,
    failure_code   VARCHAR(30),
    failure_reason VARCHAR(500),
    probe_outcome  VARCHAR(30)  NOT NULL,
    trigger_type   VARCHAR(20)  NOT NULL,

    CONSTRAINT chk_duration_non_negative CHECK (duration_ms >= 0)
);

CREATE INDEX idx_check_results_monitor_started
    ON check_results (monitor_id, started_at DESC);