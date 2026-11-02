CREATE TABLE monitor_assertions (
    id             BIGSERIAL    PRIMARY KEY,
    monitor_id     BIGINT       NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    assertion_type VARCHAR(30)  NOT NULL,
    json_path      VARCHAR(500) NOT NULL,
    operator       VARCHAR(20)  NOT NULL,
    expected_value VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assertions_monitor_id ON monitor_assertions(monitor_id);