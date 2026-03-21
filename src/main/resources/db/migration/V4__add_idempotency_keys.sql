-- Idempotency Keys für Geldüberweisungen (Stripe/PayPal-Stil)
-- Verhindert doppelte Buchungen bei Netzwerk-Timeouts und Client-Retries.
-- Eindeutig pro (idempotency_key, username) – zwei verschiedene User dürfen denselben Key nutzen.
CREATE TABLE idempotency_keys (
    id              BIGSERIAL    PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    response_body   TEXT         NOT NULL,
    response_status INT          NOT NULL DEFAULT 200,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_key_user UNIQUE (idempotency_key, username)
);
