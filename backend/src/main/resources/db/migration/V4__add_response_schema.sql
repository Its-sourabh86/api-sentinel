ALTER TABLE monitors ADD COLUMN response_schema TEXT;

ALTER TABLE monitors
    ADD CONSTRAINT chk_response_schema_size
    CHECK (response_schema IS NULL OR length(response_schema) <= 20000);