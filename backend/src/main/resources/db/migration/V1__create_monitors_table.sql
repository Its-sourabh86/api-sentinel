CREATE TABLE monitors (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100)  NOT NULL,
    url              VARCHAR(2048) NOT NULL,
    http_method      VARCHAR(10)   NOT NULL DEFAULT 'GET',
    interval_seconds INTEGER       NOT NULL,
    timeout_ms       INTEGER       NOT NULL,
    expected_status  INTEGER       NOT NULL DEFAULT 200,
    enabled          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_interval_positive CHECK (interval_seconds >= 10),
    CONSTRAINT chk_timeout_positive  CHECK (timeout_ms > 0)
);