ALTER TABLE webhook_events
    ADD COLUMN repository_id BIGINT,
    ADD CONSTRAINT fk_webhook_events_repository
        FOREIGN KEY (repository_id) REFERENCES repositories (id);

CREATE INDEX idx_webhook_events_repository_created_at
    ON webhook_events (repository_id, created_at DESC, id DESC);
