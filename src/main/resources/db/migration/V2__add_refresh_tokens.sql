-- Erstelle die refresh_tokens Tabelle für das neue Dual-Token-System
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
