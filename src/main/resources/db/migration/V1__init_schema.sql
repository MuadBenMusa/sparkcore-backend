-- Benutzerkonten (AppUser-Entity)
CREATE TABLE IF NOT EXISTS app_users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(50)  NOT NULL
);

-- Bankkonten (Account-Entity)
CREATE TABLE IF NOT EXISTS accounts (
    id         BIGSERIAL      PRIMARY KEY,
    iban       VARCHAR(34)    NOT NULL UNIQUE,
    owner_name VARCHAR(255)   NOT NULL,
    balance    NUMERIC(19, 4) NOT NULL
);

-- Transaktionshistorie (Transaction-Entity)
CREATE TABLE IF NOT EXISTS transactions (
    id            BIGSERIAL      PRIMARY KEY,
    sender_iban   VARCHAR(34)    NOT NULL,
    receiver_iban VARCHAR(34)    NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    status        VARCHAR(50)    NOT NULL,
    timestamp     TIMESTAMP      NOT NULL DEFAULT now()
);

-- Audit Log
CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(255),
    action     VARCHAR(100) NOT NULL,
    details    TEXT,
    ip_address VARCHAR(45),
    status     VARCHAR(50)  NOT NULL,
    timestamp  TIMESTAMP    NOT NULL DEFAULT now()
);
