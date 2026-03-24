-- Transactional Outbox Pattern
-- Events are written here atomically within the same transaction as the business operation.
-- A separate @Scheduled background job reads and publishes unprocessed events to Kafka.
-- This guarantees at-least-once Kafka delivery even if the broker is temporarily unavailable.

CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_type   VARCHAR(64)   NOT NULL,
    payload      TEXT          NOT NULL,  -- JSON-serialized TransactionEvent
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ   NULL       -- NULL = not yet published; set after Kafka send
);

-- Index speeds up the scheduler's query for unprocessed events
CREATE INDEX idx_outbox_unpublished ON outbox_events (published_at) WHERE published_at IS NULL;
